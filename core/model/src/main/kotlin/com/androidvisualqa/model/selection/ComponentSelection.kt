package com.androidvisualqa.model.selection

import com.androidvisualqa.model.ids.NodeId
import com.androidvisualqa.model.ids.SdkComponentId
import kotlinx.serialization.Serializable

/**
 * Links an annotation to a chosen UI component candidate.
 *
 * @property selectionId Local identifier for this selection.
 * @property annotationId References the [AnnotationEvidence] that produced this selection.
 * @property chosenNodeId The accessibility node chosen (or null if no match).
 * @property chosenSdkComponentId The SDK component chosen (or null).
 * @property candidateNodeIds Ordered list of candidate node IDs by rank.
 * @property candidateSdkComponentIds Ordered list of candidate SDK component IDs.
 * @property confidence Overall confidence score (0.0..1.0).
 * @property scoreOverlap Intersection-over-union feature score.
 * @property scoreContainment Containment feature score.
 * @property scoreCenterProximity Center-distance feature score.
 * @property scoreActionable Actionable-role feature score.
 * @property scoreSemanticRichness Semantic-richness feature score.
 * @property scoreLeafPreference Leaf-node preference score.
 * @property scoreRecentEvent Recent-event boost score.
 * @property scoreSdkEvidence SDK evidence boost score.
 * @property choiceType How the selection was determined.
 * @property evidenceSource Source of the candidate evidence.
 */
@Serializable
data class ComponentSelection(
    val selectionId: String,
    val annotationId: String,
    val chosenNodeId: NodeId? = null,
    val chosenSdkComponentId: SdkComponentId? = null,
    val candidateNodeIds: List<NodeId> = emptyList(),
    val candidateSdkComponentIds: List<SdkComponentId> = emptyList(),
    val confidence: Double = 0.0,
    val scoreOverlap: Double = 0.0,
    val scoreContainment: Double = 0.0,
    val scoreCenterProximity: Double = 0.0,
    val scoreActionable: Double = 0.0,
    val scoreSemanticRichness: Double = 0.0,
    val scoreLeafPreference: Double = 0.0,
    val scoreRecentEvent: Double = 0.0,
    val scoreSdkEvidence: Double = 0.0,
    val choiceType: SelectionChoiceType = SelectionChoiceType.NoMatch,
    val evidenceSource: EvidenceSource = EvidenceSource.Accessibility,
)

@Serializable
enum class SelectionChoiceType {
    AutoSelected,
    UserConfirmed,
    ParentOverride,
    ChildOverride,
    NoMatch,
}

@Serializable
enum class EvidenceSource {
    Accessibility,
    Sdk,
    Ocr,
    ManualRegion,
}
