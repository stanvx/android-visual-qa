package com.androidvisualqa.export.agent

import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.model.capture.CaptureFrame
import com.androidvisualqa.model.capture.CaptureMode
import com.androidvisualqa.model.capture.CaptureSession
import com.androidvisualqa.model.capture.ScreenshotMethod
import com.androidvisualqa.model.feedback.FeedbackEvidence
import com.androidvisualqa.model.ids.ReportId
import com.androidvisualqa.model.privacy.PrivacyEvidence
import com.androidvisualqa.model.privacy.RedactionRegion
import com.androidvisualqa.model.selection.ComponentSelection
import com.androidvisualqa.model.selection.SelectionChoiceType
import com.androidvisualqa.model.ReportStatus
import com.androidvisualqa.model.ids.SdkComponentId
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InstructionGeneratorTest {

    private val emptyReport: VisualFeedbackReport
        get() = VisualFeedbackReport(
            reportId = ReportId("test-id"),
            createdAt = Instant.DISTANT_PAST,
            status = ReportStatus.Saved,
            capture = CaptureSession(
                sessionId = "s1",
                startedAt = Instant.DISTANT_PAST,
                triggerSource = com.androidvisualqa.model.capture.TriggerSource.ManualImport,
                captureMode = CaptureMode.Still,
            ),
            frame = CaptureFrame(
                displayId = 0,
                windowId = 0,
                packageName = "com.example",
                widthPx = 1080,
                heightPx = 1920,
                density = 2.0f,
                rotationDegrees = 0,
                screenshotMethod = ScreenshotMethod.ManualImport,
                monotonicTimestamp = 0L,
                wallClockTimestamp = Instant.DISTANT_PAST,
            ),
            feedback = FeedbackEvidence(),
        )

    @Test
    fun `feedback with crash produces bug instruction`() {
        val report = emptyReport.copy(
            feedback = FeedbackEvidence(textBody = "The app crashed when I tapped the button"),
        )
        val instructions = InstructionGenerator.generate(report)
        assertTrue(instructions.any { it.contains("bug") }, "Expected a bug-related instruction")
    }

    @Test
    fun `feedback with exception produces bug instruction`() {
        val report = emptyReport.copy(
            feedback = FeedbackEvidence(textBody = "Got an exception: NullPointerException"),
        )
        val instructions = InstructionGenerator.generate(report)
        assertTrue(instructions.any { it.contains("bug") })
    }

    @Test
    fun `feedback with stacktrace produces bug instruction`() {
        val report = emptyReport.copy(
            feedback = FeedbackEvidence(textBody = "Stacktrace: at com.example.Main.onClick"),
        )
        val instructions = InstructionGenerator.generate(report)
        assertTrue(instructions.any { it.contains("bug") })
    }

    @Test
    fun `empty feedback produces ask-for-description`() {
        val report = emptyReport.copy(
            feedback = FeedbackEvidence(textBody = null),
        )
        val instructions = InstructionGenerator.generate(report)
        assertTrue(instructions.any { it.contains("clearer description") })
    }

    @Test
    fun `blank feedback produces ask-for-description`() {
        val report = emptyReport.copy(
            feedback = FeedbackEvidence(textBody = "   "),
        )
        val instructions = InstructionGenerator.generate(report)
        assertTrue(instructions.any { it.contains("clearer description") })
    }

    @Test
    fun `report with redactions produces verify-with-user`() {
        val report = emptyReport.copy(
            privacy = PrivacyEvidence(
                userRedactions = listOf(
                    RedactionRegion(
                        reason = "email",
                        normalizedLeft = 0.1,
                        normalizedTop = 0.1,
                        normalizedRight = 0.5,
                        normalizedBottom = 0.2,
                    ),
                ),
            ),
        )
        val instructions = InstructionGenerator.generate(report)
        assertTrue(instructions.any { it.contains("redacted") })
    }

    @Test
    fun `report with auto-redactions produces verify-with-user`() {
        val report = emptyReport.copy(
            privacy = PrivacyEvidence(
                automaticRedactions = listOf(
                    RedactionRegion(reason = "password", normalizedLeft = 0.0, normalizedTop = 0.0, normalizedRight = 1.0, normalizedBottom = 1.0),
                ),
            ),
        )
        val instructions = InstructionGenerator.generate(report)
        assertTrue(instructions.any { it.contains("redacted") })
    }

    @Test
    fun `high-confidence sdk selection produces root-cause instruction`() {
        val report = emptyReport.copy(
            selections = listOf(
                ComponentSelection(
                    selectionId = "sel1",
                    annotationId = "ann1",
                    chosenSdkComponentId = SdkComponentId("btn_login"),
                    confidence = 0.95,
                    choiceType = SelectionChoiceType.AutoSelected,
                ),
            ),
        )
        val instructions = InstructionGenerator.generate(report)
        assertTrue(instructions.any { it.contains("root cause") })
    }

    @Test
    fun `report with no salient features gets default instruction`() {
        val report = emptyReport.copy(
            feedback = FeedbackEvidence(textBody = "Looks good."),
        )
        val instructions = InstructionGenerator.generate(report)
        assertTrue(instructions.any { it.contains("report.md") })
    }

    @Test
    fun `deterministic — same input same output`() {
        val report = emptyReport.copy(
            feedback = FeedbackEvidence(textBody = "crash on tap"),
        )
        val a = InstructionGenerator.generate(report)
        val b = InstructionGenerator.generate(report)
        assertTrue(a == b, "Instructions must be deterministic")
        assertTrue(a.any { it.contains("bug") })
    }
}
