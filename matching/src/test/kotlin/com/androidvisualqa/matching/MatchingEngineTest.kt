package com.androidvisualqa.matching

import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import com.androidvisualqa.geometry.Point
import com.androidvisualqa.geometry.Polygon
import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.capture.SdkComponentSnapshot
import com.androidvisualqa.model.ids.NodeId
import com.androidvisualqa.model.ids.SdkComponentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MatchingEngineTest {

    private val engine = MatchingEngine()

    /** Screen = 1080×1920. */
    private val screen = Bounds(0.0, 0.0, 1080.0, 1920.0, CoordinateSpace.ScreenPx)

    /** A rectangle covering roughly the left half of the screen. */
    private val leftHalfSelection: Polygon<CoordinateSpace.ScreenPx> = Polygon(
        listOf(
            Point(0.0, 0.0, CoordinateSpace.ScreenPx),
            Point(540.0, 0.0, CoordinateSpace.ScreenPx),
            Point(540.0, 1920.0, CoordinateSpace.ScreenPx),
            Point(0.0, 1920.0, CoordinateSpace.ScreenPx),
        )
    )

    // ------------------------------------------------------------------
    // Helper factories
    // ------------------------------------------------------------------

    private fun node(
        id: String,
        left: Int = 10, top: Int = 10,
        right: Int = 100, bottom: Int = 100,
        clickable: Boolean = false,
        focusable: Boolean = false,
        text: String? = null,
        contentDescription: String? = null,
        viewIdRaw: String? = null,
        childIds: List<NodeId> = emptyList(),
        parentId: NodeId? = null,
        windowId: Int = 0,
    ): NodeSnapshot = NodeSnapshot(
        nodeId = NodeId(id),
        parentId = parentId,
        childIds = childIds,
        windowId = windowId,
        boundsLeft = left,
        boundsTop = top,
        boundsRight = right,
        boundsBottom = bottom,
        text = text,
        contentDescription = contentDescription,
        viewIdRaw = viewIdRaw,
        isClickable = clickable,
        isFocusable = focusable,
    )

    // ------------------------------------------------------------------
    // 1. Single node fully inside selection
    // ------------------------------------------------------------------

    @Test
    fun `single node inside selection has high confidence`() {
        // Use a node and polygon that produce high overlap/containment
        val candidate = node("a", left = 10, top = 10, right = 200, bottom = 200,
            clickable = true, text = "Hello")

        // Tight polygon exactly around the node with some margin
        val tightPoly = Polygon(
            listOf(
                Point(0.0, 0.0, CoordinateSpace.ScreenPx),
                Point(250.0, 0.0, CoordinateSpace.ScreenPx),
                Point(250.0, 250.0, CoordinateSpace.ScreenPx),
                Point(0.0, 250.0, CoordinateSpace.ScreenPx),
            )
        )

        val input = MatchingInput(
            selectionPolygon = tightPoly,
            screenBounds = screen,
            candidates = listOf(candidate),
        )
        val result = engine.rank(input)

        org.junit.jupiter.api.Assertions.assertEquals(1, result.size)
        org.junit.jupiter.api.Assertions.assertTrue(
            result[0].confidence >= 0.75,
            "Expected >= 0.75 but got ${result[0].confidence}. " +
                "overlap=${result[0].scoreOverlap} contain=${result[0].scoreContainment} " +
                "center=${result[0].scoreCenterProximity} actionable=${result[0].scoreActionable} " +
                "semantic=${result[0].scoreSemanticRichness} leaf=${result[0].scoreLeafPreference}"
        )
        org.junit.jupiter.api.Assertions.assertEquals("a", result[0].node.nodeId.value)
    }

    // ------------------------------------------------------------------
    // 2. Empty selection
    // ------------------------------------------------------------------

    @Test
    fun `empty candidate list returns empty result`() {
        val input = MatchingInput(
            selectionPolygon = leftHalfSelection,
            screenBounds = screen,
            candidates = emptyList(),
        )
        val result = engine.rank(input)

        assertTrue(result.isEmpty())
    }

    // ------------------------------------------------------------------
    // 3. Two overlapping nodes — top by score
    // ------------------------------------------------------------------

    @Test
    fun `higher scoring node ranks first among overlapping nodes`() {
        val near = node("a", left = 100, top = 100, right = 200, bottom = 200,
            clickable = true, text = "Hello")
        val far = node("b", left = 1000, top = 1000, right = 1050, bottom = 1050)

        val input = MatchingInput(
            selectionPolygon = leftHalfSelection,
            screenBounds = screen,
            candidates = listOf(far, near), // unordered
        )
        val result = engine.rank(input)

        assertEquals(2, result.size)
        assertEquals("a", result[0].node.nodeId.value) // near is better
        assertEquals("b", result[1].node.nodeId.value)
    }

    // ------------------------------------------------------------------
    // 4. Tie-breaking determinism
    // ------------------------------------------------------------------

    @Test
    fun `tie-breaking by node id ascending is deterministic`() {
        val bothInside = listOf(
            node("z", left = 10, top = 10, right = 100, bottom = 100),
            node("a", left = 10, top = 10, right = 100, bottom = 100),
            node("m", left = 10, top = 10, right = 100, bottom = 100),
        )

        val input = MatchingInput(
            selectionPolygon = leftHalfSelection,
            screenBounds = screen,
            candidates = bothInside.shuffled(),
        )
        val result = engine.rank(input)

        // All have identical features; ties broken by node.id ascending
        assertEquals("a", result[0].node.nodeId.value)
        assertEquals("m", result[1].node.nodeId.value)
        assertEquals("z", result[2].node.nodeId.value)
    }

    // ------------------------------------------------------------------
    // 5. Large-container penalty
    // ------------------------------------------------------------------

    @Test
    fun `full-screen node has its score halved`() {
        val full = node("big", left = 0, top = 0, right = 1080, bottom = 1920)
        val small = node("small", left = 10, top = 10, right = 200, bottom = 200,
            clickable = true, text = "Tap me")

        val input = MatchingInput(
            selectionPolygon = leftHalfSelection,
            screenBounds = screen,
            candidates = listOf(full, small),
        )
        val result = engine.rank(input)

        // small should outrank big despite big covering more area
        // because big gets the 0.5x penalty
        assertEquals("small", result[0].node.nodeId.value)
    }

    // ------------------------------------------------------------------
    // 6. Z-order bonus
    // ------------------------------------------------------------------

    @Test
    fun `topmost in z-order outranks identical sibling below`() {
        // Put both in the selection area with identical features
        val top = node("top", left = 10, top = 10, right = 100, bottom = 100,
            clickable = true)
        val bottom = node("bot", left = 10, top = 10, right = 100, bottom = 100,
            clickable = true)

        // Manually set windowId to differentiate z-order
        val topNode = top.copy(windowId = 1)
        val botNode = bottom.copy(windowId = 2)

        val input = MatchingInput(
            selectionPolygon = leftHalfSelection,
            screenBounds = screen,
            candidates = listOf(botNode, topNode),
            activeWindowZOrder = listOf(
                NodeId("top"),  // window 1 is topmost
                NodeId("bot"),
            ),
        )
        val result = engine.rank(input)

        // top has z-order bonus and should outrank bot
        assertEquals("top", result[0].node.nodeId.value)
        assertEquals("bot", result[1].node.nodeId.value)
    }

    // ------------------------------------------------------------------
    // 7. SDK override boosts score to at least 0.95
    // ------------------------------------------------------------------

    @Test
    fun `sdk override boosts matched candidate confidence to at least 0 dot 95`() {
        val candidate = node("a", left = 10, top = 10, right = 100, bottom = 100)

        val sdk = SdkComponentSnapshot(
            componentId = SdkComponentId("sdk-a"),
            componentType = "Button",
            boundsLeft = 10,
            boundsTop = 12,
            boundsRight = 100,
            boundsBottom = 100,
        )

        val input = MatchingInput(
            selectionPolygon = leftHalfSelection,
            screenBounds = screen,
            candidates = listOf(candidate),
            sdkOverrides = listOf(sdk),
        )
        val result = engine.rank(input)

        assertEquals(1, result.size)
        assertTrue(result[0].confidence >= 0.95)
    }

    // ------------------------------------------------------------------
    // 8. Recent event bonus
    // ------------------------------------------------------------------

    @Test
    fun `recent event node outranks identical node without event`() {
        val withEvent = node("a", left = 10, top = 10, right = 100, bottom = 100,
            clickable = true)
        val withoutEvent = node("b", left = 10, top = 10, right = 100, bottom = 100,
            clickable = true)

        val input = MatchingInput(
            selectionPolygon = leftHalfSelection,
            screenBounds = screen,
            candidates = listOf(withoutEvent, withEvent),
            recentEventNodeIds = setOf(NodeId("a")),
        )
        val result = engine.rank(input)

        assertEquals("a", result[0].node.nodeId.value)
        assertEquals("b", result[1].node.nodeId.value)
    }

    // ------------------------------------------------------------------
    // Extra: SDK bounds disagreement does NOT boost
    // ------------------------------------------------------------------

    @Test
    fun `sdk bounds disagreement does not boost confidence`() {
        val candidate = node("a", left = 10, top = 10, right = 100, bottom = 100)

        // SDK bounds far away — should NOT match
        val sdk = SdkComponentSnapshot(
            componentId = SdkComponentId("sdk-far"),
            componentType = "Button",
            boundsLeft = 500,
            boundsTop = 500,
            boundsRight = 600,
            boundsBottom = 600,
        )

        val input = MatchingInput(
            selectionPolygon = leftHalfSelection,
            screenBounds = screen,
            candidates = listOf(candidate),
            sdkOverrides = listOf(sdk),
        )
        val result = engine.rank(input)

        assertEquals(0.0, result[0].scoreSdkEvidence)
    }
}
