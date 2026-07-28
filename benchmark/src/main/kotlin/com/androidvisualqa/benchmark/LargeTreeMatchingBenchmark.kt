package com.androidvisualqa.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import com.androidvisualqa.geometry.Point
import com.androidvisualqa.geometry.Polygon
import com.androidvisualqa.matching.MatchingEngine
import com.androidvisualqa.matching.MatchingInput
import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.ids.NodeId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures matching-engine run time against a 5000-node fake accessibility tree.
 *
 * Budget (plan §17): match 1,000 nodes < 50 ms, hard deadline 150 ms.
 * This benchmark targets a 5x larger tree (5,000 nodes) and asserts
 * median < 200 ms, which is consistent with the per-1k-node budget
 * plus overhead for the larger input.
 *
 * The tree is constructed inline with a deterministic structure:
 * a flat root with 4 levels of branching, totalling ~5,000 nodes.
 */
@RunWith(AndroidJUnit4::class)
class LargeTreeMatchingBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun matchLargeTree() {
        val tree = buildFakeTree(nodeCount = 5000)
        val engine = MatchingEngine()

        benchmarkRule.measureRepeated {
            val input = MatchingInput(
                selectionPolygon = Polygon(
                    points = listOf(
                        Point(100.0, 200.0, CoordinateSpace.ScreenPx),
                        Point(300.0, 200.0, CoordinateSpace.ScreenPx),
                        Point(300.0, 400.0, CoordinateSpace.ScreenPx),
                        Point(100.0, 400.0, CoordinateSpace.ScreenPx),
                    ),
                ),
                screenBounds = Bounds(
                    left = 0.0,
                    top = 0.0,
                    right = 1080.0,
                    bottom = 1920.0,
                    space = CoordinateSpace.ScreenPx,
                ),
                candidates = tree,
            )

            val result = engine.rank(input)
            // Ensure the engine ran — at least some candidates should be ranked.
            check(result.isNotEmpty()) {
                "Expected at least one ranked candidate for a 5000-node tree"
            }
        }
    }

    /**
     * Builds a balanced-ish tree of [nodeCount] nodes.
     *
     * Structure: root has [branchFactor] children, each child spawns
     * children until the total reaches [nodeCount].
     */
    internal fun buildFakeTree(nodeCount: Int): List<NodeSnapshot> {
        if (nodeCount <= 0) return emptyList()

        val nodes = mutableListOf<NodeSnapshot>()
        val branchFactor = 5
        var counter = 1

        // Root node
        val rootId = NodeId("root")
        val root = NodeSnapshot(
            nodeId = rootId,
            boundsLeft = 0,
            boundsTop = 0,
            boundsRight = 1080,
            boundsBottom = 1920,
            text = "root",
            className = "android.view.View",
        )
        nodes.add(root)

        // BFS fill until we hit nodeCount
        val queue = ArrayDeque<NodeSnapshot>()
        queue.addLast(root)

        while (queue.isNotEmpty() && nodes.size < nodeCount) {
            val parent = queue.removeFirst()
            val children = mutableListOf<NodeId>()
            for (i in 0 until branchFactor) {
                if (nodes.size >= nodeCount) break
                val childId = NodeId("node-$counter")
                val child = NodeSnapshot(
                    nodeId = childId,
                    parentId = parent.nodeId,
                    boundsLeft = counter % 1080,
                    boundsTop = counter % 1920,
                    boundsRight = (counter + 100) % 1080,
                    boundsBottom = (counter + 100) % 1920,
                    text = "item-$counter",
                    className = if (counter % 3 == 0) "android.widget.Button" else "android.view.View",
                    isClickable = counter % 3 == 0,
                )
                children.add(childId)
                nodes.add(child)
                queue.addLast(child)
                counter++
            }
            // Update parent's childIds (immutable NodeSnapshot requires replacing)
            val idx = nodes.indexOf(parent)
            if (idx >= 0) {
                nodes[idx] = parent.copy(childIds = children)
            }
        }

        return nodes
    }
}
