package com.androidvisualqa.report

import com.androidvisualqa.model.ReportStatus
import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.model.annotation.AnnotationEvidence
import com.androidvisualqa.model.annotation.AnnotationTool
import com.androidvisualqa.model.attachment.AttachmentRef
import com.androidvisualqa.model.capture.CaptureFrame
import com.androidvisualqa.model.capture.CaptureMode
import com.androidvisualqa.model.capture.CaptureSession
import com.androidvisualqa.model.capture.ScreenshotMethod
import com.androidvisualqa.model.feedback.FeedbackEvidence
import com.androidvisualqa.model.ids.AttachmentId
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.model.ids.ReportId
import com.androidvisualqa.model.privacy.PrivacyEvidence
import com.androidvisualqa.model.privacy.SecureWindowResult
import com.androidvisualqa.model.selection.ComponentSelection
import com.androidvisualqa.model.selection.SelectionChoiceType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class ReportAssemblerTest {

    private val assembler = ReportAssembler(testClock)
    private val now = Instant.parse("2026-07-28T12:00:00Z")

    @Test
    fun `assemble produces report with correct schema version`() {
        val output = assembleDefault()
        assertEquals(1, output.schemaVersion)
    }

    @Test
    fun `assemble sets createdAt to current time`() {
        val output = assembleDefault()
        assertEquals(now, output.createdAt)
    }

    @Test
    fun `assemble produces non-blank reportId`() {
        val output = assembleDefault()
        assertNotNull(output.reportId.value)
        assertTrue(output.reportId.value.isNotBlank())
    }

    @Test
    fun `assemble preserves status`() {
        val output = assembleDefault()
        assertEquals(ReportStatus.Saved, output.status)
    }

    @Test
    fun `assemble preserves all input fields in output`() {
        val session = CaptureSession(
            sessionId = "sess-1",
            startedAt = now,
            triggerSource = com.androidvisualqa.model.capture.TriggerSource.AccessibilityOverlay,
            captureMode = CaptureMode.Still,
            lastStateName = "CapturingPixels",
            draftId = DraftId("draft-abc"),
        )
        val frame = CaptureFrame(
            displayId = 0,
            windowId = 123,
            packageName = "com.example.app",
            activityTitle = "MainActivity",
            widthPx = 1080,
            heightPx = 2400,
            density = 2.0f,
            rotationDegrees = 0,
            systemBarsTop = 100,
            systemBarsBottom = 0,
            systemBarsLeft = 0,
            systemBarsRight = 0,
            windowBoundsLeft = 0,
            windowBoundsTop = 0,
            windowBoundsRight = 1080,
            windowBoundsBottom = 2400,
            contentBoundsLeft = 0,
            contentBoundsTop = 100,
            contentBoundsRight = 1080,
            contentBoundsBottom = 2400,
            screenshotMethod = ScreenshotMethod.AccessibilityWindow,
            monotonicTimestamp = 1000L,
            wallClockTimestamp = now,
        )
        val annotations = listOf(
            AnnotationEvidence(
                annotationId = "ann-1",
                toolType = AnnotationTool.Rectangle,
                boundingBoxLeft = 0.1,
                boundingBoxTop = 0.2,
                boundingBoxRight = 0.5,
                boundingBoxBottom = 0.6,
            ),
        )
        val selections = listOf(
            ComponentSelection(
                selectionId = "sel-1",
                annotationId = "ann-1",
                choiceType = SelectionChoiceType.AutoSelected,
                confidence = 0.85,
            ),
        )
        val feedback = FeedbackEvidence(
            textBody = "The button is misaligned.",
            locale = "en-AU",
        )
        val privacy = PrivacyEvidence(
            secureWindowResult = SecureWindowResult.NotSecure,
        )
        val attachments = listOf(
            AttachmentRef(
                attachmentId = AttachmentId("att-1"),
                fileName = "original.png",
                mimeType = "image/png",
                sizeBytes = 1024,
                sha256Hex = "abc123",
                role = "original_screenshot",
            ),
        )

        val input = AssemblyInput(
            session = session,
            frame = frame,
            annotations = annotations,
            selections = selections,
            feedback = feedback,
            privacy = privacy,
            attachments = attachments,
        )
        val output = assembler.assemble(input, ReportStatus.Saved)

        assertEquals(session, output.capture)
        assertEquals(frame, output.frame)
        assertEquals(annotations, output.annotations)
        assertEquals(selections, output.selections)
        assertEquals(feedback, output.feedback)
        assertEquals(privacy, output.privacy)
        assertEquals(attachments, output.attachments)
    }

    private fun assembleDefault(): VisualFeedbackReport {
        val session = CaptureSession(
            sessionId = "sess-1",
            startedAt = now,
            triggerSource = com.androidvisualqa.model.capture.TriggerSource.AccessibilityOverlay,
            captureMode = CaptureMode.Still,
        )
        val frame = CaptureFrame(
            displayId = 0,
            windowId = 123,
            packageName = "com.example.app",
            widthPx = 1080,
            heightPx = 2400,
            density = 2.0f,
            rotationDegrees = 0,
            screenshotMethod = ScreenshotMethod.AccessibilityWindow,
            monotonicTimestamp = 1000L,
            wallClockTimestamp = now,
        )
        val feedback = FeedbackEvidence("Looks good.")
        val input = AssemblyInput(
            session = session,
            frame = frame,
            annotations = emptyList(),
            selections = emptyList(),
            feedback = feedback,
            privacy = PrivacyEvidence(),
            attachments = emptyList(),
        )
        return assembler.assemble(input, ReportStatus.Saved)
    }

    companion object {
        private val testClock = object : Clock {
            override fun now(): Instant = Instant.parse("2026-07-28T12:00:00Z")
        }
    }
}
