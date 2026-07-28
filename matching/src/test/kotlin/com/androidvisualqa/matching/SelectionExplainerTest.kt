package com.androidvisualqa.matching

import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.ids.NodeId
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SelectionExplainerTest {

    @Test
    fun `explanation includes strongest feature name and value`() {
        val candidate = RankedCandidate(
            node = NodeSnapshot(nodeId = NodeId("btn")),
            confidence = 0.85,
            scoreOverlap = 0.92,
            scoreContainment = 0.10,
            scoreCenterProximity = 0.05,
            scoreActionable = 0.0,
            scoreSemanticRichness = 0.0,
            scoreLeafPreference = 0.0,
            scoreRecentEvent = 0.0,
            scoreSdkEvidence = 0.0,
            explanation = "", // will be filled
        )
        val explanation = SelectionExplainer.explain(candidate)

        // overlap (0.92) should be the strongest feature
        assertTrue(explanation.contains("overlap"))
        assertTrue(explanation.contains("0.92"))
    }

    @Test
    fun `explanation includes resource id when present`() {
        val candidate = RankedCandidate(
            node = NodeSnapshot(
                nodeId = NodeId("btn"),
                viewIdRaw = "submit_button",
                isClickable = true,
            ),
            confidence = 0.85,
            scoreOverlap = 0.50,
            scoreContainment = 0.50,
            scoreCenterProximity = 0.0,
            scoreActionable = 1.0,
            scoreSemanticRichness = 0.33,
            scoreLeafPreference = 0.0,
            scoreRecentEvent = 0.0,
            scoreSdkEvidence = 0.0,
            explanation = "",
        )
        val explanation = SelectionExplainer.explain(candidate)

        assertTrue(explanation.contains("resource id"))
        assertTrue(explanation.contains("submit_button"))
    }

    @Test
    fun `explanation includes text when no resource id`() {
        val candidate = RankedCandidate(
            node = NodeSnapshot(
                nodeId = NodeId("tv"),
                text = "Hello World",
            ),
            confidence = 0.70,
            scoreOverlap = 0.30,
            scoreContainment = 0.40,
            scoreCenterProximity = 0.20,
            scoreActionable = 0.0,
            scoreSemanticRichness = 0.33,
            scoreLeafPreference = 0.0,
            scoreRecentEvent = 0.0,
            scoreSdkEvidence = 0.0,
            explanation = "",
        )
        val explanation = SelectionExplainer.explain(candidate)

        assertTrue(explanation.contains("text"))
        assertTrue(explanation.contains("Hello World"))
    }

    @Test
    fun `explanation includes actionable when nothing else`() {
        val candidate = RankedCandidate(
            node = NodeSnapshot(
                nodeId = NodeId("clickable"),
                isClickable = true,
            ),
            confidence = 0.50,
            scoreOverlap = 0.10,
            scoreContainment = 0.10,
            scoreCenterProximity = 0.0,
            scoreActionable = 1.0,
            scoreSemanticRichness = 0.0,
            scoreLeafPreference = 0.0,
            scoreRecentEvent = 0.0,
            scoreSdkEvidence = 0.0,
            explanation = "",
        )
        val explanation = SelectionExplainer.explain(candidate)

        assertTrue(explanation.contains("actionable"))
    }
}
