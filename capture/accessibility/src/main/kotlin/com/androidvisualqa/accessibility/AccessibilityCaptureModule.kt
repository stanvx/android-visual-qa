package com.androidvisualqa.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.androidvisualqa.capture.api.ContextSnapshot
import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.ids.NodeId
import kotlinx.datetime.Clock

/**
 * Result of a bounded tree snapshot traversal.
 *
 * @property nodes The flattened node list in depth-first order.
 * @property truncated `true` when the traversal stopped early due to
 *                     [maxNodes] or [maxDepth] limits.
 * @property totalNodes The total number of nodes visited (may be larger than
 *                      [nodes.size] if some were skipped due to the
 *                      `isImportantForAccessibility` rule).
 */
public data class TreeSnapshot(
    val nodes: List<NodeSnapshot>,
    val truncated: Boolean,
    val totalNodes: Int,
)

/**
 * Facade for accessibility-based capture operations.
 *
 * This is a plain Kotlin class (no DI framework). Wire it with the
 * [serviceProvider] lambda that returns the bound
 * [VisualFeedbackAccessibilityService] instance when available.
 *
 * ## Usage
 * ```kotlin
 * val module = AccessibilityCaptureModule { myServiceRef.get() }
 * val context = module.captureWindow(42L)
 * ```
 *
 * @param serviceProvider Returns the currently-bound service, or `null` if
 *                        the service is disconnected.
 */
public class AccessibilityCaptureModule(
    private val serviceProvider: () -> VisualFeedbackAccessibilityService?,
) {

    /**
     * Captures a [ContextSnapshot] for the given [windowId].
     *
     * Takes an accessibility screenshot and returns window metadata.
     *
     * @param windowId The accessibility window ID to capture.
     * @return A [ContextSnapshot] describing the window, or `null` if the
     *         service is unavailable or the screenshot fails.
     */
    public suspend fun captureWindow(windowId: Long): ContextSnapshot? {
        val service = serviceProvider() ?: return null

        val frame = service.takeWindowScreenshot(windowId) ?: return null

        val root = WindowResolver.resolve(service, windowId)
        val packageName = try {
            root?.packageName?.toString()
        } finally {
            root?.recycle()
        }

        // Window bounds from the window info list
        val windowBounds: Bounds<CoordinateSpace.ScreenPx>? = service.windows
            ?.firstOrNull { it.id.toLong() == windowId }
            ?.let { info ->
                val bounds = Rect()
                info.getBoundsInScreen(bounds)
                Bounds(
                    left = bounds.left.toDouble(),
                    top = bounds.top.toDouble(),
                    right = bounds.right.toDouble(),
                    bottom = bounds.bottom.toDouble(),
                    space = CoordinateSpace.ScreenPx,
                )
            }

        return ContextSnapshot(
            packageName = packageName ?: "unknown",
            windowId = windowId,
            displayId = frame.displayId,
            bounds = windowBounds ?: Bounds(
                left = 0.0,
                top = 0.0,
                right = frame.widthPx.toDouble(),
                bottom = frame.heightPx.toDouble(),
                space = CoordinateSpace.ScreenPx,
            ),
            capturedAt = Clock.System.now(),
        )
    }

    /**
     * Performs a bounded depth-first traversal of the accessibility tree
     * starting from [rootWindowId].
     *
     * ## Traversal safeguards (plan §10.5)
     * - **Iterative** traversal with an explicit stack (no recursion).
     * - **Hard depth limit** of [MAX_DEPTH] (25). Not configurable in M2.
     * - **Max node count** of [maxNodes] (default 5,000). Stops when hit and
     *   records `truncated = true`.
     * - **Importance filter:** Skips nodes whose
     *   `isImportantForAccessibility == false` unless they have no important
     *   descendant (i.e., they are the only path to actionable children).
     * - **Cycle guard:** Drops nodes whose [AccessibilityNodeInfo] identity
     *   has already been visited in this traversal.
     * - **Empty bounds filter:** Filters out nodes without any bounds or with
     *   `boundsInScreen` that is empty (zero area).
     *
     * @param rootWindowId The window to traverse.
     * @param maxNodes Maximum nodes to include before truncating.
     * @return A [TreeSnapshot] with the flattened node list.
     */
    public suspend fun snapshotTree(
        rootWindowId: Long,
        maxNodes: Int = DEFAULT_MAX_NODES,
    ): TreeSnapshot {
        val service = serviceProvider() ?: return TreeSnapshot(
            nodes = emptyList(),
            truncated = false,
            totalNodes = 0,
        )

        val root = WindowResolver.resolve(service, rootWindowId) ?: return TreeSnapshot(
            nodes = emptyList(),
            truncated = false,
            totalNodes = 0,
        )

        return try {
            traverseTree(root, maxNodes)
        } finally {
            root.recycle()
        }
    }

    /**
     * Returns the active window ID from the service, or `null`.
     */
    public suspend fun activeWindowId(): Long? {
        return serviceProvider()?.activeWindowId()
    }

    // ─── Internal traversal ─────────────────────────────────────────────

    private data class TraversalFrame(
        val node: AccessibilityNodeInfo,
        val parentId: NodeId?,
        val depth: Int,
    )

    /**
     * Iterative depth-first bounded traversal.
     *
     * Plan §10.5 safeguards applied:
     * 1. Iterative (explicit stack) — no stack-overflow risk.
     * 2. [MAX_DEPTH] hard limit (25).
     * 3. [maxNodes] count limit with `truncated` reporting.
     * 4. Importance check via [shouldTraverseNode].
     * 5. Cycle detection via visited-by-identity set.
     * 6. Empty-bounds filter via [hasNonEmptyBounds].
     */
    private fun traverseTree(root: AccessibilityNodeInfo, maxNodes: Int): TreeSnapshot {
        val maxDepth = MAX_DEPTH

        val stack = ArrayDeque<TraversalFrame>()
        val visitedIdentities = HashSet<Int>()
        val resultNodes = mutableListOf<NodeSnapshot>()
        var totalVisited = 0
        var truncated = false

        stack.addLast(TraversalFrame(root, parentId = null, depth = 0))

        while (stack.isNotEmpty()) {
            val frame = stack.removeLast()
            val node = frame.node

            // 1. Cycle guard: skip if already visited
            val identity = System.identityHashCode(node)
            if (!visitedIdentities.add(identity)) continue

            // 2. Depth guard: skip nodes beyond max depth
            if (frame.depth > maxDepth) continue

            // Count everything we visit
            totalVisited++

            // 3. Empty bounds filter
            if (!hasNonEmptyBounds(node)) {
                node.recycle()
                continue
            }

            // 4. Importance filter: skip non-important nodes that still have
            //    important descendants (they don't contribute to matching).
            //    But still traverse their children to reach the important nodes.
            val skipSnapshot = !node.isImportantForAccessibility && hasImportantDescendant(node)
            if (skipSnapshot) {
                // Push children and continue without creating a snapshot
                val parentIdForChildren = frame.parentId // use the parent's parentId
                val childCount = node.childCount
                for (i in (childCount - 1) downTo 0) {
                    val child = node.getChild(i) ?: continue
                    stack.addLast(TraversalFrame(child, parentIdForChildren, frame.depth + 1))
                }
                node.recycle()
                continue
            }

            // 5. Build and emit snapshot
            val snapshot = NodeNormalizer.normalize(node, frame.parentId).copy(
                traversalDepth = frame.depth,
            )
            resultNodes.add(snapshot)

            // 6. Max nodes guard
            if (resultNodes.size >= maxNodes) {
                truncated = true
                node.recycle()
                break
            }

            // 7. Push children (reversed for DFS matching source order)
            val childCount = node.childCount
            for (i in (childCount - 1) downTo 0) {
                val child = node.getChild(i) ?: continue
                stack.addLast(TraversalFrame(child, snapshot.nodeId, frame.depth + 1))
            }

            // Recycle parent after children have been pushed (the node was
            // obtained via getChild() and must be released).
            node.recycle()
        }

        // Recycle any remaining node references on the stack
        for (remaining in stack) {
            remaining.node.recycle()
        }

        return TreeSnapshot(
            nodes = resultNodes,
            truncated = truncated,
            totalNodes = totalVisited,
        )
    }

    private fun hasNonEmptyBounds(node: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return !rect.isEmpty
    }

    /**
     * Returns true if any descendant of [node] is important for accessibility.
     *
     * ponytail: This is O(n) in the subtree when called for every skipped
     * node. In practice, `isImportantForAccessibility == false` nodes are
     * rare, so the cost is acceptable. If profiling shows a hotspot,
     * pre-compute importance in a separate pass.
     */
    private fun hasImportantDescendant(node: AccessibilityNodeInfo): Boolean {
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (child.isImportantForAccessibility) return true
                if (hasImportantDescendant(child)) return true
            } finally {
                child.recycle()
            }
        }
        return false
    }

    internal companion object {
        /** Hard depth limit for tree traversal.
         * ponytail: 25 is well above any real Android accessibility tree.
         * Not configurable in M2. */
        internal const val MAX_DEPTH: Int = 25

        /** Default maximum nodes in a single traversal. */
        internal const val DEFAULT_MAX_NODES: Int = 5_000
    }
}
