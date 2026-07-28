package com.androidvisualqa.model

import com.androidvisualqa.model.annotation.AnnotationEvidence
import com.androidvisualqa.model.attachment.AttachmentRef
import com.androidvisualqa.model.capture.CaptureFrame
import com.androidvisualqa.model.capture.CaptureSession
import com.androidvisualqa.model.export.ExportAttempt
import com.androidvisualqa.model.feedback.FeedbackEvidence
import com.androidvisualqa.model.ids.ReportId
import com.androidvisualqa.model.privacy.PrivacyEvidence
import com.androidvisualqa.model.selection.ComponentSelection
import com.androidvisualqa.model.serialization.InstantAsIso8601Serializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Top-level evidence report for a single visual QA feedback submission.
 *
 * This is the root document in the report JSON. Every entity in the report
 * tree is @Serializable with an explicit wire format — never rely on Kotlin
 * class names for serialization discrimination.
 *
 * @property schemaVersion Schema version number. Increment on breaking changes.
 * @property reportId Unique identifier for this report.
 * @property createdAt ISO-8601 timestamp of report creation.
 * @property status Current lifecycle status.
 * @property capture The capture session that produced this report.
 * @property annotations Ordered list of annotation strokes/shapes.
 * @property selections Component selections linked to annotations.
 * @property feedback User feedback text and transcript.
 * @property privacy Privacy decisions and redactions.
 * @property attachments References to files stored alongside the report.
 * @property exports History of export attempts.
 */
@Serializable
data class VisualFeedbackReport(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val reportId: ReportId,
    @Serializable(with = InstantAsIso8601Serializer::class)
    val createdAt: Instant,
    val status: ReportStatus,
    val capture: CaptureSession,
    val frame: CaptureFrame,
    val annotations: List<AnnotationEvidence> = emptyList(),
    val selections: List<ComponentSelection> = emptyList(),
    val feedback: FeedbackEvidence,
    val privacy: PrivacyEvidence = PrivacyEvidence(),
    val attachments: List<AttachmentRef> = emptyList(),
    val exports: List<ExportAttempt> = emptyList(),
) {
    companion object {
        /** Current schema version for newly created reports. */
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}
