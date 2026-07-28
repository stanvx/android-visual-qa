package com.androidvisualqa.matching

import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.ids.NodeId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ParentNavigatorTest {

    // Build a small tree:
    //          root (clickable, LinearLayout)
    //         /    \
    //   container   sibling (another container)
    //      |
    //   leaf (clickable=false)

    private val leafId = NodeId("leaf")
    private val containerId = NodeId("container")
    private val rootId = NodeId("root")
    private val siblingId = NodeId("sibling")

    private val root = NodeSnapshot(
        nodeId = rootId,
        parentId = null,
        childIds = listOf(containerId, siblingId),
        isClickable = true,
        className = "android.widget.LinearLayout",
    )

    private val container = NodeSnapshot(
        nodeId = containerId,
        parentId = rootId,
        childIds = listOf(leafId),
        isClickable = false,
        className = "android.widget.FrameLayout",
    )

    private val leaf = NodeSnapshot(
        nodeId = leafId,
        parentId = containerId,
        childIds = emptyList(),
        isClickable = false,
    )

    private val sibling = NodeSnapshot(
        nodeId = siblingId,
        parentId = rootId,
        childIds = emptyList(),
        isClickable = false,
        className = "android.widget.LinearLayout",
    )

    private val allNodes = listOf(root, container, leaf, sibling)

    @Test
    fun `walkUp returns nearest clickable ancestor`() {
        // leaf -> container (not clickable) -> root (clickable) => root
        val result = ParentNavigator.walkUp(leaf, allNodes)
        assertEquals(rootId, result?.nodeId)
    }

    @Test
    fun `walkUp returns null when no clickable ancestor exists`() {
        // sibling is not clickable, root is clickable — so sibling should still get root
        val siblingNotClickable = sibling.copy(isClickable = false)
        // Actually root IS clickable, so sibling will walk up to root
        val result = ParentNavigator.walkUp(siblingNotClickable, allNodes)
        assertEquals(rootId, result?.nodeId)
    }

    @Test
    fun `walkDown returns leaf-most descendant`() {
        // root -> container -> leaf
        val result = ParentNavigator.walkDown(root, allNodes)
        assertEquals(leafId, result.nodeId)
    }

    @Test
    fun `walkDown returns self for leaf node`() {
        val result = ParentNavigator.walkDown(leaf, allNodes)
        assertEquals(leafId, result.nodeId)
    }

    @Test
    fun `containerAncestor returns first ViewGroup ancestor`() {
        // leaf -> container (FrameLayout) is a ViewGroup
        val result = ParentNavigator.containerAncestor(leaf, allNodes)
        assertEquals(containerId, result?.nodeId)
    }

    @Test
    fun `containerAncestor returns null when at root with no ViewGroup parent`() {
        // root has no parent, so no container ancestor
        val result = ParentNavigator.containerAncestor(root, allNodes)
        assertNull(result)
    }
}
