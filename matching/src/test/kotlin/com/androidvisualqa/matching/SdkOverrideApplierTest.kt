package com.androidvisualqa.matching

import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.capture.SdkComponentSnapshot
import com.androidvisualqa.model.ids.NodeId
import com.androidvisualqa.model.ids.SdkComponentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SdkOverrideApplierTest {

    @Test
    fun `matching bounds boosts confidence to at least 0 dot 95`() {
        val candidate = NodeSnapshot(
            nodeId = NodeId("a"),
            boundsLeft = 10, boundsTop = 10,
            boundsRight = 100, boundsBottom = 100,
        )
        val sdk = SdkComponentSnapshot(
            componentId = SdkComponentId("sdk-a"),
            componentType = "Button",
            boundsLeft = 10, boundsTop = 12,
            boundsRight = 100, boundsBottom = 100,
        )

        val ranked = listOf(
            RankedCandidate(
                node = candidate,
                confidence = 0.50,
                scoreOverlap = 0.5, scoreContainment = 0.5,
                scoreCenterProximity = 0.0, scoreActionable = 0.0,
                scoreSemanticRichness = 0.0, scoreLeafPreference = 0.0,
                scoreRecentEvent = 0.0, scoreSdkEvidence = 0.0,
                explanation = "test",
            )
        )

        val result = SdkOverrideApplier.apply(ranked, listOf(sdk), listOf(candidate))
        assertEquals(1, result.size)
        assertTrue(result[0].confidence >= 0.95)
        assertEquals(1.0, result[0].scoreSdkEvidence)
    }

    @Test
    fun `bounds disagreement does not boost confidence`() {
        val candidate = NodeSnapshot(
            nodeId = NodeId("a"),
            boundsLeft = 10, boundsTop = 10,
            boundsRight = 100, boundsBottom = 100,
        )
        val sdk = SdkComponentSnapshot(
            componentId = SdkComponentId("sdk-far"),
            componentType = "Button",
            boundsLeft = 500, boundsTop = 500,
            boundsRight = 600, boundsBottom = 600,
        )

        val ranked = listOf(
            RankedCandidate(
                node = candidate,
                confidence = 0.50,
                scoreOverlap = 0.5, scoreContainment = 0.5,
                scoreCenterProximity = 0.0, scoreActionable = 0.0,
                scoreSemanticRichness = 0.0, scoreLeafPreference = 0.0,
                scoreRecentEvent = 0.0, scoreSdkEvidence = 0.0,
                explanation = "test",
            )
        )

        val result = SdkOverrideApplier.apply(ranked, listOf(sdk), listOf(candidate))
        assertEquals(0.50, result[0].confidence)
        assertEquals(0.0, result[0].scoreSdkEvidence)
    }

    @Test
    fun `findMatchingNodeIds returns empty set for no overrides`() {
        val result = SdkOverrideApplier.findMatchingNodeIds(emptyList(), emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findMatchingNodeIds matches correct node`() {
        val candidate = NodeSnapshot(
            nodeId = NodeId("a"),
            boundsLeft = 10, boundsTop = 10,
            boundsRight = 100, boundsBottom = 100,
        )
        val sdk = SdkComponentSnapshot(
            componentId = SdkComponentId("sdk"),
            componentType = "Button",
            boundsLeft = 10, boundsTop = 10,
            boundsRight = 100, boundsBottom = 100,
        )

        val result = SdkOverrideApplier.findMatchingNodeIds(
            listOf(sdk), listOf(candidate)
        )
        assertEquals(setOf(NodeId("a")), result)
    }

    @Test
    fun `findMatchingNodeIds does not match non-aligning bounds`() {
        val candidate = NodeSnapshot(
            nodeId = NodeId("a"),
            boundsLeft = 10, boundsTop = 10,
            boundsRight = 100, boundsBottom = 100,
        )
        val sdk = SdkComponentSnapshot(
            componentId = SdkComponentId("sdk"),
            componentType = "Button",
            boundsLeft = 500, boundsTop = 10,
            boundsRight = 600, boundsBottom = 100,
        )

        val result = SdkOverrideApplier.findMatchingNodeIds(
            listOf(sdk), listOf(candidate)
        )
        assertFalse(NodeId("a") in result)
    }
}
