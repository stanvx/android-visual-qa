package com.androidvisualqa.matching

import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.capture.SdkComponentSnapshot
import com.androidvisualqa.model.ids.NodeId

/**
 * Applies first-party SDK evidence to boost candidate scores.
 *
 * The tolerance for bounds agreement is **4 dp** (density-independent pixels).
 * Since both [NodeSnapshot] and [SdkComponentSnapshot] bounds are in screen
 * pixels, we compare them directly. 4 dp at ~3.0 density is roughly 12 px;
 * using a flat 12 px tolerance is a conservative approximation.
 *
 * ponytail: flat 12px tolerance approximates 4dp at 3x density.
 * Upgrade to a proper dp-to-px conversion when the matching module gains
 * access to display metrics.
 */
object SdkOverrideApplier {

    /** Tolerance in screen pixels (≈ 4 dp at 3x density). */
    private const val BOUNDS_TOLERANCE_PX = 12.0

    /**
     * Return the set of [NodeId]s from [candidates] whose bounds agree with a
     * [SdkComponentSnapshot] from [sdkOverrides] within [BOUNDS_TOLERANCE_PX].
     */
    fun findMatchingNodeIds(
        sdkOverrides: List<SdkComponentSnapshot>,
        candidates: List<NodeSnapshot>,
    ): Set<NodeId> {
        if (sdkOverrides.isEmpty()) return emptySet()

        val matched = LinkedHashSet<NodeId>()

        for (sdk in sdkOverrides) {
            for (candidate in candidates) {
                if (boundsAgree(sdk, candidate)) {
                    matched.add(candidate.nodeId)
                }
            }
        }

        return matched
    }

    /**
     * Apply the SDK override boost: for matched nodes, replace the candidate's
     * confidence with `max(currentConfidence, 0.95)`.
     *
     * @return updated list, or the original if no overrides apply.
     */
    fun apply(
        ranked: List<RankedCandidate>,
        sdkOverrides: List<SdkComponentSnapshot>,
        candidates: List<NodeSnapshot>,
    ): List<RankedCandidate> {
        if (sdkOverrides.isEmpty()) return ranked
        val boostedIds = findMatchingNodeIds(sdkOverrides, candidates)
        if (boostedIds.isEmpty()) return ranked

        return ranked.map { candidate ->
            if (candidate.node.nodeId in boostedIds) {
                val boostedConfidence = maxOf(candidate.confidence, 0.95)
                candidate.copy(
                    confidence = boostedConfidence.coerceIn(0.0, 1.0),
                    scoreSdkEvidence = 1.0,
                )
            } else {
                candidate
            }
        }
    }

    /**
     * Check if the SDK snapshot bounds agree with the node bounds within tolerance.
     */
    private fun boundsAgree(sdk: SdkComponentSnapshot, node: NodeSnapshot): Boolean {
        val leftOk = kotlin.math.abs(sdk.boundsLeft.toDouble() - node.boundsLeft.toDouble()) <= BOUNDS_TOLERANCE_PX
        val topOk = kotlin.math.abs(sdk.boundsTop.toDouble() - node.boundsTop.toDouble()) <= BOUNDS_TOLERANCE_PX
        val rightOk = kotlin.math.abs(sdk.boundsRight.toDouble() - node.boundsRight.toDouble()) <= BOUNDS_TOLERANCE_PX
        val bottomOk = kotlin.math.abs(sdk.boundsBottom.toDouble() - node.boundsBottom.toDouble()) <= BOUNDS_TOLERANCE_PX
        return leftOk && topOk && rightOk && bottomOk
    }
}
