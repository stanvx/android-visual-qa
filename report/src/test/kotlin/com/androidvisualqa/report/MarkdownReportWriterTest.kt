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
import com.androidvisualqa.model.ids.ReportId
import com.androidvisualqa.model.privacy.PrivacyEvidence
import com.androidvisualqa.model.selection.ComponentSelection
import com.androidvisualqa.model.selection.SelectionChoiceType
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class MarkdownReportWriterTest {
    @TempDir
    lateinit var tempDir: Path

    private val writer = MarkdownReportWriter()

    @Test
    fun `markdown contains report header`() {
        val md = writer.write(fixedReport)
        assertTrue(md.contains("# Visual Feedback Report"))
    }

    @Test
    fun `markdown contains report id`() {
        val md = writer.write(fixedReport)
        assertTrue(md.contains("test-report-id"))
    }

    @Test
    fun `markdown contains all section headers`() {
        val md = writer.write(fixedReport)
        assertTrue(md.contains("## Feedback"))
        assertTrue(md.contains("## Annotations"))
        assertTrue(md.contains("## Component Selections"))
        assertTrue(md.contains("## Privacy"))
        assertTrue(md.contains("## Attachments"))
    }

    @Test
    fun `markdown contains annotation bullet`() {
        val md = writer.write(fixedReport)
        assertTrue(md.contains("tool="))
        assertTrue(md.contains("annotation-1"))
    }

    @Test
    fun `markdown contains selection bullet`() {
        val md = writer.write(fixedReport)
        assertTrue(md.contains("choice="))
        assertTrue(md.contains("confidence="))
    }

    @Test
    fun `markdown contains attachment bullet`() {
        val md = writer.write(fixedReport)
        assertTrue(md.contains("original.png"))
        assertTrue(md.contains("image/png"))
    }

    @Test
    fun `markdown write atomically to file(@TempDir)`() {
        val path = tempDir.resolve("report.md")
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
                    annotationId = "annotation-1",
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
                    annotationId = "annotation-1",
                    choiceType = SelectionChoiceType.AutoSelected,
                    confidence = 0.85,
                ),
            ),
            feedback = FeedbackEvidence(textBody = "The button is too small."),
            privacy = PrivacyEvidence(),
            attachments = listOf(
                AttachmentRef(
                    attachmentId = AttachmentId("att-1"),
                    fileName = "original.png",
                    mimeType = "image/png",
                    sizeBytes = 1024,
                    sha256Hex = "abcdef123456",
                    role = "original_screenshot",
                ),
            ),
        )
}
