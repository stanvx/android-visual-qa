package com.androidvisualqa.matching

import com.androidvisualqa.model.selection.SelectionChoiceType

/**
 * Decision produced by [DecisionPolicy.apply].
 *
 * @property choiceType The decision mode.
 * @property top The highest-ranked candidate (null if no candidates).
 * @property candidates The candidates to surface (subset of ranked list).
 */
data class Decision(
    val choiceType: SelectionChoiceType,
    val top: RankedCandidate?,
    val candidates: List<RankedCandidate>,
)

/**
 * Applies the confidence-based decision policy described in plan §12.
 *
 * | Confidence range | ChoiceType          | Surface               |
 * |------------------|---------------------|-----------------------|
 * | >= 0.75          | AutoSelected        | Top 1                 |
 * | 0.45 – 0.74      | ManualReview        | Top 3                 |
 * | < 0.45           | ManualRegion        | Empty                 |
 * | No candidates    | NoMatch             | Empty                 |
 */
object DecisionPolicy {

    /** Auto-select threshold. */
    private const val AUTO_SELECT_THRESHOLD = 0.75

    /** Manual-review threshold. */
    private const val MANUAL_REVIEW_THRESHOLD = 0.45

    /**
     * Apply the policy to the ranked candidate list.
     */
    fun apply(ranked: List<RankedCandidate>): Decision {
        val top = ranked.firstOrNull()
        return when {
            top == null -> Decision(SelectionChoiceType.NoMatch, null, emptyList())
            top.confidence >= AUTO_SELECT_THRESHOLD ->
                Decision(SelectionChoiceType.AutoSelected, top, listOf(top))
            top.confidence >= MANUAL_REVIEW_THRESHOLD ->
                Decision(SelectionChoiceType.UserConfirmed, top, ranked.take(3))
            else ->
                Decision(SelectionChoiceType.NoMatch, null, emptyList())
        }
    }
}
