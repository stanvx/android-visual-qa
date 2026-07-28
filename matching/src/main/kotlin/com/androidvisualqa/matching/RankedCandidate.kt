package com.androidvisualqa.matching

import com.androidvisualqa.model.capture.NodeSnapshot

/**
 * A ranked candidate produced by [MatchingEngine].
 *
 * All scores are in [0.0, 1.0]. [confidence] is the final weighted score
 * (clamped to [0.0, 1.0]).
 *
 * @property node The accessibility node being scored.
 * @property confidence Overall confidence (weighted sum of all feature scores).
 * @property scoreOverlap Lasso-to-node intersection-over-union.
 * @property scoreContainment Fraction of the node's area contained by the lasso polygon.
 * @property scoreCenterProximity Normalised center-distance (1.0 = exactly at lasso centre).
 * @property scoreActionable Whether the node is clickable or has a recognised interactive role.
 * @property scoreSemanticRichness Count of non-empty text / content description / resource-id fields.
 * @property scoreLeafPreference 1.0 if the node has no children in the candidate list.
 * @property scoreRecentEvent 1.0 if the node received a recent focus/click event.
 * @property scoreSdkEvidence 1.0 if a matching SDK snapshot exists and bounds agree.
 * @property explanation Human-readable summary of the strongest feature contributions.
 */
data class RankedCandidate(
    val node: NodeSnapshot,
    val confidence: Double,
    val scoreOverlap: Double,
    val scoreContainment: Double,
    val scoreCenterProximity: Double,
    val scoreActionable: Double,
    val scoreSemanticRichness: Double,
    val scoreLeafPreference: Double,
    val scoreRecentEvent: Double,
    val scoreSdkEvidence: Double,
    val explanation: String,
)
