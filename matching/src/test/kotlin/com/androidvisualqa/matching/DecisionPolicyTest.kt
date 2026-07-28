package com.androidvisualqa.matching

import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.ids.NodeId
import com.androidvisualqa.model.selection.SelectionChoiceType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DecisionPolicyTest {

    private fun candidate(confidence: Double): RankedCandidate = RankedCandidate(
        node = NodeSnapshot(nodeId = NodeId("test")),
        confidence = confidence,
        scoreOverlap = 0.0,
        scoreContainment = 0.0,
        scoreCenterProximity = 0.0,
        scoreActionable = 0.0,
        scoreSemanticRichness = 0.0,
        scoreLeafPreference = 0.0,
        scoreRecentEvent = 0.0,
        scoreSdkEvidence = 0.0,
        explanation = "test",
    )

    @Test
    fun `empty list returns NoMatch`() {
        val d = DecisionPolicy.apply(emptyList())
        assertEquals(SelectionChoiceType.NoMatch, d.choiceType)
        assertEquals(null, d.top)
        assertEquals(emptyList<RankedCandidate>(), d.candidates)
    }

    @Test
    fun `confidence above 0 dot 75 returns AutoSelected with top one`() {
        val list = listOf(candidate(0.80), candidate(0.50))
        val d = DecisionPolicy.apply(list)
        assertEquals(SelectionChoiceType.AutoSelected, d.choiceType)
        assertEquals(0.80, d.top!!.confidence)
        assertEquals(1, d.candidates.size)
    }

    @Test
    fun `confidence between 0 dot 45 and 0 dot 74 returns ManualReview with top 3`() {
        val list = listOf(
            candidate(0.60),
            candidate(0.55),
            candidate(0.50),
            candidate(0.40),
        )
        val d = DecisionPolicy.apply(list)
        assertEquals(SelectionChoiceType.UserConfirmed, d.choiceType)
        assertEquals(3, d.candidates.size)
    }

    @Test
    fun `confidence below 0 dot 45 returns NoMatch`() {
        val list = listOf(candidate(0.30))
        val d = DecisionPolicy.apply(list)
        assertEquals(SelectionChoiceType.NoMatch, d.choiceType)
        assertEquals(null, d.top)
        assertEquals(emptyList<RankedCandidate>(), d.candidates)
    }

    @Test
    fun `boundary at exactly 0 dot 75 is AutoSelected`() {
        val d = DecisionPolicy.apply(listOf(candidate(0.75)))
        assertEquals(SelectionChoiceType.AutoSelected, d.choiceType)
    }

    @Test
    fun `boundary at exactly 0 dot 45 is UserConfirmed`() {
        val d = DecisionPolicy.apply(listOf(candidate(0.45)))
        assertEquals(SelectionChoiceType.UserConfirmed, d.choiceType)
    }
}
