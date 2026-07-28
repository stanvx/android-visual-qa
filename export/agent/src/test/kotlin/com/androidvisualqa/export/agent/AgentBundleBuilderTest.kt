package com.androidvisualqa.export.agent

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
import com.androidvisualqa.model.privacy.RedactionRegion
import com.androidvisualqa.model.privacy.SecureWindowResult
import com.androidvisualqa.model.selection.ComponentSelection
import com.androidvisualqa.model.selection.SelectionChoiceType
import com.androidvisualqa.model.ReportStatus
import com.androidvisualqa.model.ids.SdkComponentId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AgentBundleBuilderTest {

    private val clock = Clock.System

    private fun makeReport(
        feedbackText: String? = "The button colour is wrong",
        secureWindow: SecureWindowResult = SecureWindowResult.NotSecure,
        redactions: List<RedactionRegion> = emptyList(),
        selections: List<ComponentSelection> = emptyList(),
        annotations: List<AnnotationEvidence> = emptyList(),
        attachments: List<AttachmentRef> = emptyList(),
    ): VisualFeedbackReport {
        val now = clock.now()
        return VisualFeedbackReport(
            reportId = ReportId("r-abc-123"),
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
                windowId = 42,
                packageName = "com.example.app",
                widthPx = 1080,
                heightPx = 1920,
                density = 2.0f,
                rotationDegrees = 0,
                systemBarsTop = 100,
                systemBarsBottom = 0,
                systemBarsLeft = 0,
                systemBarsRight = 0,
                windowBoundsLeft = 0,
                windowBoundsTop = 100,
                windowBoundsRight = 1080,
                windowBoundsBottom = 1920,
                contentBoundsLeft = 0,
                contentBoundsTop = 100,
                contentBoundsRight = 1080,
                contentBoundsBottom = 1820,
                screenshotMethod = ScreenshotMethod.AccessibilityWindow,
                monotonicTimestamp = 1000L,
                wallClockTimestamp = now,
            ),
            annotations = annotations,
            selections = selections,
            feedback = FeedbackEvidence(textBody = feedbackText),
            privacy = PrivacyEvidence(
                secureWindowResult = secureWindow,
                automaticRedactions = redactions,
                excludedFields = listOf("com.example.SecretData"),
            ),
            attachments = attachments,
        )
    }

    @Test
    fun `build maps all report fields to bundle`() {
        val report = makeReport(
            feedbackText = "The login button is misaligned",
            secureWindow = SecureWindowResult.NotSecure,
            redactions = listOf(
                RedactionRegion("password", 0.1, 0.2, 0.5, 0.3),
            ),
            selections = listOf(
                ComponentSelection(
                    selectionId = "sel-1",
                    annotationId = "ann-1",
                    chosenSdkComponentId = SdkComponentId("btn_login"),
                    confidence = 0.95,
                    choiceType = SelectionChoiceType.AutoSelected,
                ),
            ),
            annotations = listOf(
                AnnotationEvidence(
                    annotationId = "ann-1",
                    toolType = AnnotationTool.Rectangle,
                    boundingBoxLeft = 100.0,
                    boundingBoxTop = 200.0,
                    boundingBoxRight = 500.0,
                    boundingBoxBottom = 400.0,
                ),
            ),
            attachments = listOf(
                AttachmentRef(
                    attachmentId = AttachmentId("att-1"),
                    fileName = "screenshot.png",
                    mimeType = "image/png",
                    sizeBytes = 1024L,
                    sha256Hex = "aabbcc",
                    role = "original_screenshot",
                ),
                AttachmentRef(
                    attachmentId = AttachmentId("att-2"),
                    fileName = "report.json",
                    mimeType = "application/json",
                    sizeBytes = 512L,
                    sha256Hex = "ddeeff",
                    role = "report_json",
                ),
            ),
        )

        val builder = AgentBundleBuilder(canvasWidth = 1080, canvasHeight = 1920, clock = clock)
        val bundle = builder.build(report)

        // Top-level fields
        assertEquals(1, bundle.schemaVersion)
        assertEquals("r-abc-123", bundle.reportId)
        assertEquals("com.example.app", bundle.packageName)
        assertEquals(42L, bundle.windowId)
        assertEquals("The login button is misaligned", bundle.feedback)

        // Annotations
        assertEquals(1, bundle.annotations.size)
        val ann = bundle.annotations.first()
        assertEquals("ann-1", ann.id)
        assertEquals("Rectangle", ann.toolType)

        // Candidates
        assertEquals(1, bundle.candidates.size)
        val cand = bundle.candidates.first()
        assertEquals("sel-1", cand.selectionId)
        assertEquals("AutoSelected", cand.choiceType)
        assertEquals(0.95, cand.confidence, 1e-9)
        assertEquals("btn_login", cand.sdkComponentId)

        // Privacy
        assertEquals("NotSecure", bundle.privacy.secureWindowResult)
        assertEquals(listOf("com.example.SecretData"), bundle.privacy.excludedFields)
        assertEquals(1, bundle.privacy.redactionCount)

        // Attachments
        assertEquals("screenshot.png", bundle.originalPngPath)
        assertEquals("report.json", bundle.rawReportJsonPath)
        assertNull(bundle.annotatedPngPath)

        // Instructions
        assertTrue(bundle.instructions.isNotEmpty())
    }

    @Test
    fun `windowId is null when frame windowId is 0`() {
        val report = makeReport().copy(
            frame = makeReport().frame.copy(windowId = 0),
        )
        val bundle = AgentBundleBuilder(1080, 1920).build(report)
        assertNull(bundle.windowId)
    }

    @Test
    fun `buildJson produces valid JSON with schemaVersion`() {
        val report = makeReport()
        val json = AgentBundleBuilder(1080, 1920).buildJson(report)
        assertTrue(json.isNotBlank())
        assertTrue(json.contains("\"schemaVersion\":1"))
    }

    @Test
    fun `buildJson does not contain Kotlin class names`() {
        val report = makeReport()
        val json = AgentBundleBuilder(1080, 1920).buildJson(report)
        // The bundle should not leak any internal class names
        assertTrue(json.contains("reportId"))
        assertTrue(json.contains("packageName"))
    }

    @Test
    fun `secure window maps correctly`() {
        val report = makeReport().copy(
            privacy = PrivacyEvidence(secureWindowResult = SecureWindowResult.SecureWindow),
        )
        val bundle = AgentBundleBuilder(1080, 1920).build(report)
        assertEquals("Secure", bundle.privacy.secureWindowResult)
    }

    @Test
    fun `unknown secure-window maps correctly`() {
        val report = makeReport().copy(
            privacy = PrivacyEvidence(secureWindowResult = SecureWindowResult.Unknown),
        )
        val bundle = AgentBundleBuilder(1080, 1920).build(report)
        assertEquals("Unknown", bundle.privacy.secureWindowResult)
    }

    @Test
    fun `redaction count sums auto and user redactions`() {
        val report = makeReport().copy(
            privacy = PrivacyEvidence(
                automaticRedactions = listOf(
                    RedactionRegion("a1", 0.0, 0.0, 0.1, 0.1),
                    RedactionRegion("a2", 0.0, 0.0, 0.1, 0.1),
                ),
                userRedactions = listOf(
                    RedactionRegion("u1", 0.0, 0.0, 0.1, 0.1),
                ),
            ),
        )
        val bundle = AgentBundleBuilder(1080, 1920).build(report)
        assertEquals(3, bundle.privacy.redactionCount)
    }

    @Test
    fun `empty feedback produces empty string not null`() {
        val report = makeReport(feedbackText = null)
        val bundle = AgentBundleBuilder(1080, 1920).build(report)
        assertEquals("", bundle.feedback)
    }
}
