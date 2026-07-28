package com.androidvisualqa.benchmark.internal

import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import com.androidvisualqa.geometry.Point
import com.androidvisualqa.geometry.Polygon
import com.androidvisualqa.matching.MatchingInput
import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.ids.NodeId

/**
 * Builds a balanced-ish tree of [nodeCount] nodes for benchmark testing.
 *
 * Structure: root has [branchFactor] children, each child spawns
 * children until the total reaches [nodeCount].
 *
 * @param nodeCount target number of nodes.  Actual count may be ≤ [nodeCount]
 *   due to branching constraints.
 * @return list of [NodeSnapshot] entries forming a single-root tree.
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

/**
 * Creates a [MatchingInput] seeded from [tree] with a rectangular selection
 * covering roughly the top-left quadrant of a 1080×1920 screen.
 */
internal fun matchingInputForTree(tree: List<NodeSnapshot>): MatchingInput =
    MatchingInput(
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
