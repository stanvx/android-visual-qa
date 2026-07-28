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
import java.nio.file.Files
import com.androidvisualqa.model.privacy.PrivacyEvidence
import com.androidvisualqa.model.selection.ComponentSelection
import com.androidvisualqa.model.selection.SelectionChoiceType
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class JsonReportWriterTest {
    @TempDir
    lateinit var tempDir: Path

    private val writer = JsonReportWriter()

    @Test
    fun `write produces json with schemaVersion 1`() {
        val json = writer.write(fixedReport)
        assertTrue(json.contains("\"schemaVersion\":1"))
    }

    @Test
    fun `write produces json with reportId`() {
        val json = writer.write(fixedReport)
        assertTrue(json.contains("\"reportId\""))
    }

    @Test
    fun `write produces json with status discriminator`() {
        val json = writer.write(fixedReport)
        assertTrue(json.contains("\"status\":\"Saved\""))
    }

    @Test
    fun `write atomically to file(@TempDir)`() {
        val path = tempDir.resolve("report.json")
        val result = kotlinx.coroutines.runBlocking { writer.write(fixedReport, path) }
        assertTrue(result.isSuccess)
        assertTrue(Files.exists(path))
    }

    companion object {
        private val now = Instant.parse("2026-07-28T12:00:00Z")
    }

    private val fixedReport = VisualFeedbackReport(
            schemaVersion = 1,
            reportId = ReportId("test-report-id"),
            createdAt = now,
            status = ReportStatus.Saved,
            capture = CaptureSession(
                sessionId = "sess-1",
                startedAt = now,
                triggerSource = com.androidvisualqa.model.capture.TriggerSource.AccessibilityOverlay,
                captureMode = CaptureMode.Still,
                draftId = DraftId("draft-1"),
            ),
            frame = CaptureFrame(
                displayId = 0,
                windowId = 123,
                packageName = "com.example.test",
                widthPx = 1080,
                heightPx = 2400,
                density = 2.0f,
                rotationDegrees = 0,
                screenshotMethod = ScreenshotMethod.AccessibilityWindow,
                monotonicTimestamp = 1000L,
                wallClockTimestamp = now,
            ),
            annotations = listOf(
                AnnotationEvidence(
                    annotationId = "ann-1",
                    toolType = AnnotationTool.Rectangle,
                    boundingBoxLeft = 0.1,
                    boundingBoxTop = 0.2,
                    boundingBoxRight = 0.5,
                    boundingBoxBottom = 0.6,
                ),
            ),
            selections = listOf(
                ComponentSelection(
                    selectionId = "sel-1",
                    annotationId = "ann-1",
                    choiceType = SelectionChoiceType.AutoSelected,
                    confidence = 0.85,
                ),
            ),
            feedback = FeedbackEvidence("Looks good"),
            privacy = PrivacyEvidence(),
            attachments = listOf(
                AttachmentRef(
                    attachmentId = AttachmentId("att-1"),
                    fileName = "original.png",
                    mimeType = "image/png",
                    sizeBytes = 1024,
                    sha256Hex = "abc123",
                    role = "original_screenshot",
                ),
            ),
        )
}
