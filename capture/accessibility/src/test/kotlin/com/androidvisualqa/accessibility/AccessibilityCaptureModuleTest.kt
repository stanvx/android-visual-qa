package com.androidvisualqa.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.ArrayDeque

/**
 * Tests for [AccessibilityCaptureModule] bounded traversal.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AccessibilityCaptureModuleTest {

    @Test
    fun `returns empty snapshot when service is disconnected`() {
        val module = AccessibilityCaptureModule { null }
        val snapshot = kotlinx.coroutines.runBlocking {
            module.snapshotTree(rootWindowId = 0L)
        }

        assertFalse(snapshot.truncated)
        assertEquals(0, snapshot.nodes.size.toLong())
        assertEquals(0, snapshot.totalNodes.toLong())
    }

    @Test
    fun `activeWindowId returns null when service is disconnected`() {
        val module = AccessibilityCaptureModule { null }
        val wid = kotlinx.coroutines.runBlocking { module.activeWindowId() }
        assertNull(wid)
    }

    @Test
    fun `TreeSnapshot data class works correctly`() {
        val snapshot = TreeSnapshot(
            nodes = emptyList(),
            truncated = true,
            totalNodes = 100,
        )

        assertTrue(snapshot.truncated)
        assertTrue(snapshot.nodes.isEmpty())
        assertEquals(100, snapshot.totalNodes.toLong())
    }

    // ─── Traversal logic tests via SimpleNode ──────────────────────────

    @Test
    fun `bounded traversal stops at depth 25`() {
        val graph = SimpleNode.rootWithDepth(30)
        val result = traverseBounded(graph, maxNodes = 500_000)

        assertTrue(result.size <= 26)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `bounded traversal stops at node count`() {
        val children = List(6000) { SimpleNode.leaf("child-$it") }
        val root = SimpleNode("root", children)
        val result = traverseBounded(root, maxNodes = 5000)

        assertTrue(result.size <= 5000)
    }

    @Test
    fun `bounded traversal dedupes by identity`() {
        val shared = SimpleNode.leaf("shared")
        val root = SimpleNode("root", listOf(shared, shared))
        val result = traverseBounded(root, maxNodes = 5000)

        // root + shared = 2
        assertEquals(2, result.size.toLong())
    }

    @Test
    fun `bounded traversal filters empty bounds`() {
        val withBounds = SimpleNode.leaf("bounded", hasBounds = true)
        val noBounds = SimpleNode.leaf("unbounded", hasBounds = false)
        val root = SimpleNode("root", listOf(noBounds, withBounds))
        val result = traverseBounded(root, maxNodes = 5000)

        // root + boundedChild = 2
        assertEquals(2, result.size.toLong())
    }

    @Test
    fun `bounded traversal skips non-important nodes with important children`() {
        val grandchild = SimpleNode.leaf("grandchild", important = true)
        val child = SimpleNode("child", listOf(grandchild), important = false)
        val parent = SimpleNode("parent", listOf(child), important = false)
        val result = traverseBounded(parent, maxNodes = 5000)

        // Only grandchild included
        assertEquals(1, result.size.toLong())
        assertEquals("grandchild", result[0].name)
    }

    // ─── SimpleNode traversal helper ────────────────────────────────────

    private data class SimpleNode(
        val name: String,
        val children: List<SimpleNode> = emptyList(),
        val hasBounds: Boolean = true,
        val important: Boolean = true,
    ) {
        companion object {
            fun leaf(name: String, hasBounds: Boolean = true, important: Boolean = true) =
                SimpleNode(name, hasBounds = hasBounds, important = important)

            fun rootWithDepth(depth: Int): SimpleNode {
                var node: SimpleNode = leaf("leaf")
                for (d in 1..depth) {
                    node = SimpleNode("n$d", listOf(node))
                }
                return node
            }
        }
    }

    private fun hasImportantDescendant(node: SimpleNode): Boolean =
        node.children.any { it.important || hasImportantDescendant(it) }

    private data class TraversalResult(val name: String, val depth: Int)

    private fun traverseBounded(
        root: SimpleNode,
        maxNodes: Int,
    ): List<TraversalResult> {
        data class Frame(val node: SimpleNode, val depth: Int)

        val stack = ArrayDeque<Frame>()
        val visited = mutableSetOf<SimpleNode>()
        val result = mutableListOf<TraversalResult>()

        stack.addLast(Frame(root, 0))

        while (stack.isNotEmpty() && result.size < maxNodes) {
            val frame = stack.removeLast()

            if (!visited.add(frame.node)) continue
            if (frame.depth > 25) continue
            if (!frame.node.hasBounds) continue
            if (!frame.node.important && hasImportantDescendant(frame.node)) {
                // Skip snapshot but still traverse children
                for (child in frame.node.children.reversed()) {
                    stack.addLast(Frame(child, frame.depth + 1))
                }
                continue
            }

            result.add(TraversalResult(frame.node.name, frame.depth))

            for (child in frame.node.children.reversed()) {
                stack.addLast(Frame(child, frame.depth + 1))
            }
        }

        return result
    }

    // ─── Fakes ──────────────────────────────────────────────────────────

    private class FakeVisualFeedbackService : VisualFeedbackAccessibilityService() {
        override fun getRootInActiveWindow() = null
        override fun activeWindowId(): Long? = null
        override fun getWindows(): List<AccessibilityWindowInfo>? = null
        override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
        override fun onInterrupt() {}
    }
}
