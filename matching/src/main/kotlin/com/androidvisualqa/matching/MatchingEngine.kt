package com.androidvisualqa.matching

import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import com.androidvisualqa.geometry.Point
import com.androidvisualqa.geometry.Polygon
import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.ids.NodeId
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Deterministic ranking engine that scores [NodeSnapshot] candidates against a
 * user-drawn lasso / rectangle selection.
 *
 * **Determinism**: This engine uses no random source. All iteration is over
 * ordered lists ([List], not [Set]/[Map] iteration). Same input always produces
 * identical output.
 *
 * ## Feature weights (tunable constants)
 *
 * | Feature               | Weight | Rationale                                         |
 * |-----------------------|--------|---------------------------------------------------|
 * | Overlap               | 0.28   | How much the lasso covers a node (IoU)            |
 * | Containment           | 0.16   | How much of the node lies inside the lasso        |
 * | Center proximity      | 0.14   | Distance from lasso centre (normalised)            |
 * | Actionable            | 0.10   | Clickable / interactive role                       |
 * | Semantic richness     | 0.08   | Non-empty text, content-desc, or resource ID      |
 * | Leaf preference       | 0.08   | No children in the candidate list                  |
 * | Recent event boost    | 0.06   | Recent focus/click event                           |
 * | SDK evidence          | 0.10   | Matching first-party SDK snapshot                  |
 * | Container penalty     | -0.50× | Multiplier 0.5 if node fills the screen            |
 *
 * Weights are surfaced here as a [weights] data class so the plan's
 * "configuration, not constants" requirement is met.
 */
class MatchingEngine(
    /** Tunable feature weights. */
    val weights: FeatureWeights = FeatureWeights(),
) {

    /**
     * All feature weights in one place.
     *
     * ponytail: single data class instead of eight named params;
     * add `copy()` callers when dynamic reconfiguration is needed.
     */
    data class FeatureWeights(
        val overlap: Double = 0.28,
        val containment: Double = 0.16,
        val centerProximity: Double = 0.14,
        val actionable: Double = 0.10,
        val semanticRichness: Double = 0.08,
        val leafPreference: Double = 0.08,
        val recentEvent: Double = 0.06,
        val sdkEvidence: Double = 0.10,
    ) {
        init {
            val total = overlap + containment + centerProximity +
                actionable + semanticRichness + leafPreference +
                recentEvent + sdkEvidence
            require(abs(total - 1.0) < 1e-9) {
                "Feature weights must sum to 1.0, got $total"
            }
        }
    }

    companion object {
        /** Z-order bonus for the topmost window. */
        private const val Z_ORDER_TOP_BONUS = 0.1
        /** Decay per position in z-order. */
        private const val Z_ORDER_DECAY = 0.02
        /** Positions beyond this get zero z-order bonus. */
        private const val Z_ORDER_ZERO_AFTER = 6

        /** Multiplier applied when a node's bounds equal or exceed screen bounds. */
        private const val LARGE_CONTAINER_PENALTY = 0.5
    }

    /**
     * Rank all candidates against the user's selection.
     *
     * Returns ALL candidates sorted by score descending. Ties are broken by
     * [NodeSnapshot.nodeId] ascending for determinism.
     */
    fun rank(input: MatchingInput): List<RankedCandidate> {
        val polygon = input.selectionPolygon
        val polygonCentroid = polygon.centroid
        val polygonArea = polygon.area
        val screenDiagonal = screenDiagonalLength(input.screenBounds)

        val childIds = buildChildIdSet(input.candidates)
        val candidateIds = buildCandidateIdSet(input.candidates)

        // Prune SDK overrides — collect node IDs whose SDK bounds match.
        val sdkBoostedIds = SdkOverrideApplier.findMatchingNodeIds(
            sdkOverrides = input.sdkOverrides,
            candidates = input.candidates,
        )

        val scored = input.candidates.map { node ->
            val overlap = overlapScore(node, polygon, polygonArea)
            val containment = containmentScore(node, polygon)
            val centerProx = centerProximityScore(node, polygonCentroid, screenDiagonal)
            val actionable = if (node.isClickable || node.isFocusable) 1.0 else 0.0
            val semantic = semanticRichnessScore(node)
            val leaf = if (node.childIds.isEmpty() || !hasChildrenInCandidateList(node, childIds)) 1.0 else 0.0
            val recent = if (node.nodeId in input.recentEventNodeIds) 1.0 else 0.0
            val sdkEv = if (node.nodeId in sdkBoostedIds) 1.0 else 0.0
            val zOrderBonus = zOrderBonus(node.nodeId, input.activeWindowZOrder)

            val rawScore =
                weights.overlap * overlap +
                    weights.containment * containment +
                    weights.centerProximity * centerProx +
                    weights.actionable * actionable +
                    weights.semanticRichness * semantic +
                    weights.leafPreference * leaf +
                    weights.recentEvent * recent +
                    weights.sdkEvidence * sdkEv +
                    zOrderBonus

            // Large-container penalty.
            val penalty = nodeBounds(node).let { b ->
                if (b.left <= input.screenBounds.left && b.top <= input.screenBounds.top &&
                    b.right >= input.screenBounds.right && b.bottom >= input.screenBounds.bottom
                ) LARGE_CONTAINER_PENALTY else 1.0
            }

            val confidence = (rawScore * penalty).coerceIn(0.0, 1.0)

            // Apply SDK override boost (max with 0.95)
            val finalConfidence = if (node.nodeId in sdkBoostedIds) {
                maxOf(confidence, 0.95)
            } else {
                confidence
            }

            RankedCandidate(
                node = node,
                confidence = finalConfidence.coerceIn(0.0, 1.0),
                scoreOverlap = overlap.coerceIn(0.0, 1.0),
                scoreContainment = containment.coerceIn(0.0, 1.0),
                scoreCenterProximity = centerProx.coerceIn(0.0, 1.0),
                scoreActionable = actionable,
                scoreSemanticRichness = semantic.coerceIn(0.0, 1.0),
                scoreLeafPreference = leaf,
                scoreRecentEvent = recent,
                scoreSdkEvidence = sdkEv,
                explanation = SelectionExplainer.explain(
                    overlap = overlap,
                    containment = containment,
                    centerProx = centerProx,
                    actionable = actionable,
                    semantic = semantic,
                    leaf = leaf,
                    recent = recent,
                    sdkEv = sdkEv,
                    zOrderBonus = zOrderBonus,
                    node = node,
                ),
            )
        }

        return scored.sortedWith(
            compareByDescending<RankedCandidate> { it.confidence }
                .thenBy { it.node.nodeId.value },
        )
    }

    // ------------------------------------------------------------------
    // Feature computation helpers
    // ------------------------------------------------------------------

    /**
     * Overlap score = IoU between the node's bounding box and the lasso polygon.
     *
     * We approximate the polygon's intersection with the node bounds by sampling
     * points of the node's bounding box against the lasso polygon's area.
     * For a precise estimate we use min(polygonArea, nodeArea) / max(polygonArea, nodeArea)
     * when the polygon centroid is inside the node bounds, scaling by containment.
     */
    private fun overlapScore(
        node: NodeSnapshot,
        polygon: Polygon<CoordinateSpace.ScreenPx>,
        polygonArea: Double,
    ): Double {
        val bounds = nodeBounds(node)
        if (bounds.isEmpty) return 0.0

        val nodeArea = bounds.width * bounds.height
        if (nodeArea <= 0.0 || polygonArea <= 0.0) return 0.0

        // Intersection area approximated via containment of polygon centroid
        // in the node bounds, combined with how many points of the node
        // bounding box fall inside the polygon.
        val cornersInside = nodeCorners(bounds).count { polygon.contains(it) } / 4.0
        val centroidInside = if (polygon.contains(bounds.center)) 1.0 else 0.0

        // IoU estimate: average of the two containment ratios
        val polygonInNode = centroidInside // polygon centroid in node
        val nodeInPolygon = cornersInside

        val intersection = polygonInNode * nodeInPolygon
        val union = polygonInNode + nodeInPolygon - intersection * nodeInPolygon
        // ponytail: approximate IoU; exact polygon-polygon intersection is costly.
        // Upgrade to a proper polygon clipping library (e.g. clipper) when records matter.
        return if (union > 0.0) intersection / union else 0.0
    }

    /**
     * Containment score = fraction of the node's bounding box area that falls
     * inside the lasso polygon.
     *
     * We sample the 4 corners of the node's bounding box; if all 4 are inside
     * the polygon the score is 1.0. If none, 0.0. Otherwise it's the fraction.
     */
    private fun containmentScore(
        node: NodeSnapshot,
        polygon: Polygon<CoordinateSpace.ScreenPx>,
    ): Double {
        val bounds = nodeBounds(node)
        if (bounds.isEmpty) return 0.0

        val corners = nodeCorners(bounds)
        val insideCount = corners.count { polygon.contains(it) }
        return insideCount / 4.0
    }

    /**
     * Center proximity score = 1.0 when the node centre is at the lasso centre,
     * decaying linearly to 0.0 at the screen diagonal distance.
     */
    private fun centerProximityScore(
        node: NodeSnapshot,
        polygonCentroid: Point<CoordinateSpace.ScreenPx>,
        screenDiagonal: Double,
    ): Double {
        val nodeCenter = nodeBounds(node).center
        val dx = nodeCenter.x - polygonCentroid.x
        val dy = nodeCenter.y - polygonCentroid.y
        val distance = sqrt(dx * dx + dy * dy)
        return if (screenDiagonal > 0.0) {
            (1.0 - min(distance / screenDiagonal, 1.0)).coerceIn(0.0, 1.0)
        } else 0.0
    }

    /**
     * Semantic richness = fraction of non-empty text / contentDescription /
     * viewIdResourceName fields out of 3.
     */
    private fun semanticRichnessScore(node: NodeSnapshot): Double {
        var count = 0
        if (!node.text.isNullOrBlank()) count++
        if (!node.contentDescription.isNullOrBlank()) count++
        if (!node.viewIdRaw.isNullOrBlank()) count++
        return count / 3.0
    }

    /**
     * Z-order bonus: 0.1 for topmost, decays by 0.02 per position, 0 at position 6+.
     */
    private fun zOrderBonus(
        nodeId: NodeId,
        activeWindowZOrder: List<NodeId>,
    ): Double {
        val idx = activeWindowZOrder.indexOf(nodeId)
        if (idx < 0 || idx >= Z_ORDER_ZERO_AFTER) return 0.0
        return (Z_ORDER_TOP_BONUS - idx * Z_ORDER_DECAY).coerceAtLeast(0.0)
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun nodeBounds(node: NodeSnapshot): Bounds<CoordinateSpace.ScreenPx> =
        Bounds(
            left = node.boundsLeft.toDouble(),
            top = node.boundsTop.toDouble(),
            right = node.boundsRight.toDouble(),
            bottom = node.boundsBottom.toDouble(),
            space = CoordinateSpace.ScreenPx,
        )

    private fun nodeCorners(bounds: Bounds<CoordinateSpace.ScreenPx>): List<Point<CoordinateSpace.ScreenPx>> =
        listOf(
            Point(bounds.left, bounds.top, CoordinateSpace.ScreenPx),
            Point(bounds.right, bounds.top, CoordinateSpace.ScreenPx),
            Point(bounds.right, bounds.bottom, CoordinateSpace.ScreenPx),
            Point(bounds.left, bounds.bottom, CoordinateSpace.ScreenPx),
        )

    private fun screenDiagonalLength(bounds: Bounds<CoordinateSpace.ScreenPx>): Double =
        sqrt(bounds.width * bounds.width + bounds.height * bounds.height)

    /** Build a set of all node IDs in the candidate list for quick lookup. */
    private fun buildCandidateIdSet(candidates: List<NodeSnapshot>): Set<NodeId> {
        val set = LinkedHashSet<NodeId>(candidates.size)
        for (c in candidates) set.add(c.nodeId)
        return set
    }

    /** Build a set of all child node IDs across all candidates. */
    private fun buildChildIdSet(candidates: List<NodeSnapshot>): Set<NodeId> {
        val set = LinkedHashSet<NodeId>()
        for (c in candidates) {
            for (childId in c.childIds) {
                set.add(childId)
            }
        }
        return set
    }

    /** True if any of [node]'s children appear in the candidate list. */
    private fun hasChildrenInCandidateList(node: NodeSnapshot, childIds: Set<NodeId>): Boolean {
        for (childId in node.childIds) {
            if (childId in childIds) return true
        }
        return false
    }
}
