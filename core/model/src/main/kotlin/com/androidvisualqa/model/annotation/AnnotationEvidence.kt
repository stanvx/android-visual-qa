package com.androidvisualqa.model.annotation

import kotlinx.serialization.Serializable

/**
 * An annotation stroke or shape on the frozen frame.
 *
 * Points are stored in normalized frame coordinates (0.0..1.0) so they
 * remain valid across display sizes and rotation. The display transform
 * at creation time is recorded for round-trip verification.
 *
 * @property annotationId Local identifier for this annotation.
 * @property toolType The tool used (pen, rectangle, arrow, etc.).
 * @property strokePoints Ordered points in normalized coordinates.
 * @property pressures Per-point pressure samples where available.
 * @property tilts Per-point tilt samples where available.
 * @property boundingBoxLeft Normalized bounding box (0.0..1.0).
 * @property boundingBoxTop Normalized bounding box (0.0..1.0).
 * @property boundingBoxRight Normalized bounding box (0.0..1.0).
 * @property boundingBoxBottom Normalized bounding box (0.0..1.0).
 * @property convexHull Convex hull polygon in normalized coordinates.
 * @property displayRotationDegrees Display rotation when stroke was created.
 * @property displayWidthPx Display pixel width at creation.
 * @property displayHeightPx Display pixel height at creation.
 * @property linkedComment Optional text or transcript segment reference.
 * @property linkedComments Comments attached to this annotation. The legacy
 * linkedComment field remains for readers and writers of schema v1 evidence.
 * @property undoGroupId Strokes that should undo together share a group ID.
 */
@Serializable
data class AnnotationEvidence(
    val annotationId: String,
    val toolType: AnnotationTool,
    val strokePoints: List<NormalizedPoint> = emptyList(),
    val pressures: List<Float> = emptyList(),
    val tilts: List<Float> = emptyList(),
    val boundingBoxLeft: Double = 0.0,
    val boundingBoxTop: Double = 0.0,
    val boundingBoxRight: Double = 0.0,
    val boundingBoxBottom: Double = 0.0,
    val convexHull: List<NormalizedPoint> = emptyList(),
    val displayRotationDegrees: Int = 0,
    val displayWidthPx: Int = 0,
    val displayHeightPx: Int = 0,
    val linkedComment: String? = null,
    val linkedComments: List<AnnotationComment> = emptyList(),
    val undoGroupId: String? = null,
)

/** A persisted comment linked to one annotation ID by its containing evidence. */
@Serializable
data class AnnotationComment(
    val commentId: String,
    val text: String,
)

/**
 * A 2D point in normalized frame coordinates (0.0..1.0).
 */
@Serializable
data class NormalizedPoint(
    val x: Double,
    val y: Double,
)

/**
 * Tool types available in the annotation editor.
 */
@Serializable
enum class AnnotationTool {
    Pen,
    Highlighter,
    Rectangle,
    Ellipse,
    Arrow,
    Lasso,
    Eraser,
    TextNote,
    Blur,
}
