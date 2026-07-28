package com.androidvisualqa.matching

import com.androidvisualqa.model.capture.NodeSnapshot

/**
 * Parent/child navigation helpers for the UI layer.
 *
 * Walks the flattened accessibility tree to find interactive parents,
 * leaf-most descendants, and ViewGroup container ancestors.
 */
object ParentNavigator {

    /**
     * Walk up from [node] to find the nearest ancestor that is clickable
     * and contains [node] in its subtree.
     *
     * Returns `null` if no such ancestor exists.
     */
    fun walkUp(node: NodeSnapshot, allNodes: List<NodeSnapshot>): NodeSnapshot? {
        val map = allNodes.associateBy { it.nodeId }
        var current = node
        // Guard against infinite loops in malformed trees.
        val seen = mutableSetOf(current.nodeId)
        while (true) {
            val parentId = current.parentId ?: return null
            if (parentId in seen) return null // cycle detected
            val parent = map[parentId] ?: return null
            seen.add(parentId)
            if (parent.isClickable && isAncestorOf(parent, node.nodeId, map)) {
                return parent
            }
            current = parent
        }
    }

    /**
     * Walk down from [node] to find the leaf-most descendant.
     *
     * Returns [node] itself if it has no children.
     */
    fun walkDown(node: NodeSnapshot, allNodes: List<NodeSnapshot>): NodeSnapshot {
        val map = allNodes.associateBy { it.nodeId }
        var current = node
        val seen = mutableSetOf(current.nodeId)
        while (current.childIds.isNotEmpty()) {
            val firstChild = current.childIds.firstOrNull() ?: return current
            if (firstChild in seen) return current // cycle
            seen.add(firstChild)
            val child = map[firstChild] ?: return current
            current = child
        }
        return current
    }

    /**
     * Find the first ancestor whose [className] starts with an Android ViewGroup
     * package prefix (e.g. `android.widget.*`, `androidx.compose.ui.*`).
     *
     * Returns `null` if no such ancestor exists.
     */
    fun containerAncestor(node: NodeSnapshot, allNodes: List<NodeSnapshot>): NodeSnapshot? {
        val map = allNodes.associateBy { it.nodeId }
        var current = node
        val seen = mutableSetOf(current.nodeId)
        while (true) {
            val parentId = current.parentId ?: return null
            if (parentId in seen) return null
            val parent = map[parentId] ?: return null
            seen.add(parentId)
            if (isViewGroupClass(parent.className)) {
                return parent
            }
            current = parent
        }
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /** True if [potentialAncestor] is an ancestor of [descendantId] (or is the same node). */
    private fun isAncestorOf(
        potentialAncestor: NodeSnapshot,
        descendantId: com.androidvisualqa.model.ids.NodeId,
        map: Map<com.androidvisualqa.model.ids.NodeId, NodeSnapshot>,
    ): Boolean {
        if (potentialAncestor.nodeId == descendantId) return true
        // Walk up from descendant to see if we reach potentialAncestor
        var currentId = descendantId
        val seen = mutableSetOf(currentId)
        while (true) {
            val node = map[currentId] ?: return false
            val parentId = node.parentId ?: return false
            if (parentId in seen) return false
            seen.add(parentId)
            if (parentId == potentialAncestor.nodeId) return true
            currentId = parentId
        }
    }

    /** True if [className] looks like an Android ViewGroup. */
    private fun isViewGroupClass(className: String?): Boolean {
        if (className == null) return false
        // Compose: "androidx.compose.ui.layout.FlexBox" etc.
        // Android: "android.widget.LinearLayout", "android.widget.FrameLayout", etc.
        return className.startsWith("android.widget.") ||
            (className.startsWith("androidx.compose.") && !className.endsWith("ComposeView")) ||
            className.startsWith("android.view.ViewGroup") ||
            className.startsWith("androidx.constraintlayout.") ||
            className.startsWith("androidx.recyclerview.")
    }
}
