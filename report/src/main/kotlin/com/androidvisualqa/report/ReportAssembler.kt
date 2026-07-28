package com.androidvisualqa.report

import com.androidvisualqa.model.ReportStatus
import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.model.annotation.AnnotationEvidence
import com.androidvisualqa.model.attachment.AttachmentRef
import com.androidvisualqa.model.capture.CaptureFrame
import com.androidvisualqa.model.capture.CaptureSession
import com.androidvisualqa.model.feedback.FeedbackEvidence
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.model.ids.ReportId
import com.androidvisualqa.model.privacy.PrivacyEvidence
import com.androidvisualqa.model.selection.ComponentSelection
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

/**
 * All inputs required to produce a [VisualFeedbackReport].
 */
public data class AssemblyInput(
    public val session: CaptureSession,
    public val frame: CaptureFrame,
    public val annotations: List<AnnotationEvidence>,
    public val selections: List<ComponentSelection>,
    public val feedback: FeedbackEvidence,
    public val privacy: PrivacyEvidence,
    public val attachments: List<AttachmentRef>,
)

/**
 * Assembles a new immutable [VisualFeedbackReport] from the evidence provided
 * in [AssemblyInput].
 *
 * @param clock Source of time; defaults to [Clock.System].
 */
public class ReportAssembler(
    private val clock: Clock = Clock.System,
) {
    public fun assemble(input: AssemblyInput, status: ReportStatus): VisualFeedbackReport {
        val now: Instant = clock.now()
        val reportId = ReportId(UUID.randomUUID().toString())

        return VisualFeedbackReport(
            schemaVersion = VisualFeedbackReport.CURRENT_SCHEMA_VERSION,
            reportId = reportId,
            createdAt = now,
            status = status,
            capture = input.session,
            frame = input.frame,
            annotations = input.annotations,
            selections = input.selections,
            feedback = input.feedback,
            privacy = input.privacy,
            attachments = input.attachments,
        )
    }
}
