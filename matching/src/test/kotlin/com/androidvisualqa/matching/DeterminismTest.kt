package com.androidvisualqa.matching

import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import com.androidvisualqa.geometry.Point
import com.androidvisualqa.geometry.Polygon
import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.ids.NodeId
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

class DeterminismTest {

    @Test
    fun `100 runs on same input produce byte-identical results`() {
        val screen = Bounds(0.0, 0.0, 1080.0, 1920.0, CoordinateSpace.ScreenPx)

        val poly = Polygon(
            listOf(
                Point(100.0, 100.0, CoordinateSpace.ScreenPx),
                Point(400.0, 100.0, CoordinateSpace.ScreenPx),
                Point(400.0, 400.0, CoordinateSpace.ScreenPx),
                Point(100.0, 400.0, CoordinateSpace.ScreenPx),
            )
        )

        val candidates = listOf(
            NodeSnapshot(
                nodeId = NodeId("c"),
                boundsLeft = 10, boundsTop = 10,
                boundsRight = 200, boundsBottom = 200,
            ),
            NodeSnapshot(
                nodeId = NodeId("a"),
                boundsLeft = 50, boundsTop = 50,
                boundsRight = 150, boundsBottom = 150,
                isClickable = true,
                text = "Hello",
            ),
            NodeSnapshot(
                nodeId = NodeId("b"),
                boundsLeft = 500, boundsTop = 500,
                boundsRight = 600, boundsBottom = 600,
            ),
        )

        val input = MatchingInput(
            selectionPolygon = poly,
            screenBounds = screen,
            candidates = candidates,
            activeWindowZOrder = listOf(
                NodeId("a"),
                NodeId("b"),
                NodeId("c"),
            ),
            recentEventNodeIds = setOf(NodeId("a")),
        )

        val engine = MatchingEngine()

        // Run 100 times
        var firstBytes: ByteArray? = null
        for (run in 0 until 100) {
            val result = engine.rank(input)
            val bos = ByteArrayOutputStream()
            ObjectOutputStream(bos).use { oos ->
                oos.writeObject(result.map { it.node.nodeId.value + it.confidence.toString() })
            }
            val bytes = bos.toByteArray()

            if (firstBytes == null) {
                firstBytes = bytes
            } else {
                assertArrayEquals(firstBytes, bytes, "Mismatch on run $run")
            }
        }
    }
}
