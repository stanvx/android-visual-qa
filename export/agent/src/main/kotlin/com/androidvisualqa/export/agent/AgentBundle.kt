package com.androidvisualqa.export.agent

import kotlinx.serialization.Serializable

/**
 * Agent-friendly JSON bundle that encapsulates a [VisualFeedbackReport] for
 * consumption by downstream AI tools (Claude, GPT, IDE plugins, etc.).
 *
 * All spatial coordinates are normalised to the range [0.0, 1.0] relative to
 * the original canvas dimensions supplied at build time.
 *
 * @property schemaVersion Bundle schema version. Increment on breaking changes.
 * @property reportId Opaque string identifier from the source report.
 * @property createdAt ISO-8601 timestamp of the source report.
 * @property packageName Android package name of the captured app.
 * @property windowId Accessibility window ID (null if unknown).
 * @property feedback Free-form user feedback text.
 * @property annotations Annotation strokes/shapes with normalised bounds.
 * @property candidates Component selection candidates.
 * @property sdkComponents Enrichment data from the Compose SDK.
 * @property privacy Privacy summary.
 * @property instructions Suggested next steps for an AI agent.
 * @property rawReportJsonPath Relative path to the raw report JSON (if bundled).
 * @property originalPngPath Relative path to the original screenshot (if bundled).
 * @property annotatedPngPath Relative path to the annotated screenshot (if bundled).
 */
@Serializable
data class AgentBundle(
    val schemaVersion: Int = 1,
    val reportId: String,
    val createdAt: String,
    val packageName: String,
    val windowId: Long?,
    val feedback: String,
    val annotations: List<AgentAnnotation>,
    val candidates: List<AgentCandidate>,
    val sdkComponents: List<AgentSdkComponent>,
    val privacy: AgentPrivacy,
    val instructions: List<String>,
    val rawReportJsonPath: String? = null,
    val originalPngPath: String? = null,
    val annotatedPngPath: String? = null,
)

/**
 * An annotation stroke or shape with bounds normalised to [0.0, 1.0].
 *
 * @property id Local identifier for this annotation.
 * @property toolType The tool used ("Rectangle", "Lasso", etc.).
 * @property boundsNormalised Bounding box in normalised coordinates.
 * @property colour Optional #AARRGGBB colour string.
 */
@Serializable
data class AgentAnnotation(
    val id: String,
    val toolType: String,
    val boundsNormalized: AgentBounds,
    val color: String? = null,
)

/**
 * A component candidate produced by the matching engine.
 *
 * @property selectionId Local identifier for this selection.
 * @property choiceType How the selection was determined ("AutoSelected", etc.).
 * @property confidence Overall confidence score (0.0..1.0).
 * @property nodeId Accessibility node ID (if matched).
 * @property sdkComponentId SDK component ID (if matched).
 * @property explanation Human-readable explanation of the match.
 */
@Serializable
data class AgentCandidate(
    val selectionId: String,
    val choiceType: String,
    val confidence: Double,
    val nodeId: String? = null,
    val sdkComponentId: String? = null,
    val explanation: String? = null,
)

/**
 * An SDK-enriched component with privacy metadata.
 *
 * @property componentId Stable developer-provided key.
 * @property componentType Semantic component type.
 * @property boundsNormalized On-screen bounds normalised to [0.0, 1.0].
 * @property privacySensitivity Privacy classification ("Public", "Credentials", etc.).
 * @property routePath Screen or navigation route path.
 * @property semantics Arbitrary key-value semantic metadata.
 */
@Serializable
data class AgentSdkComponent(
    val componentId: String,
    val componentType: String? = null,
    val boundsNormalized: AgentBounds,
    val privacySensitivity: String? = null,
    val routePath: String? = null,
    val semantics: Map<String, String> = emptyMap(),
)

/**
 * An axis-aligned bounding box in normalised coordinates [0.0, 1.0].
 */
@Serializable
data class AgentBounds(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
)

/**
 * Privacy summary for the bundle.
 *
 * @property secureWindowResult Whether the captured window had FLAG_SECURE.
 * @property excludedFields Field paths excluded from serialization.
 * @property redactionCount Total number of redacted regions.
 */
@Serializable
data class AgentPrivacy(
    val secureWindowResult: String,
    val excludedFields: List<String>,
    val redactionCount: Int,
)
