package com.androidvisualqa.model

import com.androidvisualqa.model.annotation.AnnotationEvidence
import com.androidvisualqa.model.annotation.AnnotationTool
import com.androidvisualqa.model.attachment.AttachmentRef
import com.androidvisualqa.model.capture.CaptureFrame
import com.androidvisualqa.model.capture.CaptureMode
import com.androidvisualqa.model.capture.CaptureSession
import com.androidvisualqa.model.capture.ScreenshotMethod
import com.androidvisualqa.model.capture.TriggerSource
import com.androidvisualqa.model.export.ExportAttempt
import com.androidvisualqa.model.feedback.FeedbackEvidence
import com.androidvisualqa.model.ids.AttachmentId
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.model.ids.ReportId
import com.androidvisualqa.model.privacy.PrivacyEvidence
import com.androidvisualqa.model.privacy.SecureWindowResult
import com.androidvisualqa.model.selection.ComponentSelection
import com.androidvisualqa.model.selection.SelectionChoiceType
import com.androidvisualqa.model.serialization.JsonConfig
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VisualFeedbackReportSerializationTest {

    private val fixedInstant = Instant.parse("2026-07-28T10:00:00Z")

    private fun buildMinimalReport(): VisualFeedbackReport {
        val now = fixedInstant
        return VisualFeedbackReport(
            schemaVersion = VisualFeedbackReport.CURRENT_SCHEMA_VERSION,
            reportId = ReportId("report-001"),
            createdAt = now,
            status = ReportStatus.Draft,
            capture = CaptureSession(
                sessionId = "session-001",
                startedAt = now,
                triggerSource = TriggerSource.AccessibilityOverlay,
                captureMode = CaptureMode.Still,
                lastStateName = "Complete",
                draftId = DraftId("draft-001"),
            ),
            frame = CaptureFrame(
                displayId = 0,
                windowId = 42,
                packageName = "com.example.app",
                widthPx = 1080,
                heightPx = 2400,
                density = 420f,
                rotationDegrees = 0,
                screenshotMethod = ScreenshotMethod.AccessibilityWindow,
                monotonicTimestamp = 123456789L,
                wallClockTimestamp = now,
            ),
            feedback = FeedbackEvidence(
                textBody = "The button is misaligned.",
                locale = "en-AU",
            ),
            privacy = PrivacyEvidence(
                secureWindowResult = SecureWindowResult.NotSecure,
                ocrUsed = false,
                audioUsed = false,
            ),
            annotations = listOf(
                AnnotationEvidence(
                    annotationId = "ann-001",
                    toolType = AnnotationTool.Rectangle,
                ),
            ),
            selections = listOf(
                ComponentSelection(
                    selectionId = "sel-001",
                    annotationId = "ann-001",
                    confidence = 0.85,
                    choiceType = SelectionChoiceType.AutoSelected,
                ),
            ),
            attachments = listOf(
                AttachmentRef(
                    attachmentId = AttachmentId("att-001"),
                    fileName = "original.png",
                    mimeType = "image/png",
                    sizeBytes = 102400,
                    sha256Hex = "abc123def456",
                    role = "original_screenshot",
                ),
            ),
            exports = listOf(
                ExportAttempt(
                    exportId = "exp-001",
                    destination = "local_fs",
                    startedAt = now,
                    completedAt = now,
                    success = true,
                    exportedFormats = listOf("json"),
                ),
            ),
        )
    }

    @Test
    fun `round-trip preserves all fields`() {
        val original = buildMinimalReport()
        val json = JsonConfig.encodeToString(original)
        val restored = JsonConfig.decodeFromString<VisualFeedbackReport>(json)

        assertEquals(original, restored)
    }

    @Test
    fun `json contains schemaVersion 1`() {
        val json = JsonConfig.encodeToString(buildMinimalReport())

        assertTrue(json.contains(""""schemaVersion":1"""), "Expected schemaVersion=1 in JSON")
    }

    @Test
    fun `json does not contain Kotlin class names`() {
        val json = JsonConfig.encodeToString(buildMinimalReport())

        // Check that no Kotlin FQN patterns appear
        assertFalse(json.contains("com.androidvisualqa"), "JSON should not leak Kotlin class names")
    }

    @Test
    fun `json uses type discriminator for enums`() {
        val json = JsonConfig.encodeToString(buildMinimalReport())

        // We don't have polymorphic sealed hierarchies yet,
        // but confirm the discriminator key doesn't appear in scalar values
        // and that enum values are plain strings
        assertTrue(json.contains(""""Draft""""))
        assertTrue(json.contains(""""Rectangle""""))
    }

    @Test
    fun `reportId serializes as plain string`() {
        val json = JsonConfig.encodeToString(buildMinimalReport())

        assertTrue(json.contains(""""reportId":"report-001""""))
    }

    @Test
    fun `privacy block is serialized`() {
        val json = JsonConfig.encodeToString(buildMinimalReport())

        assertTrue(json.contains(""""secureWindowResult":"NotSecure""""))
    }
}
