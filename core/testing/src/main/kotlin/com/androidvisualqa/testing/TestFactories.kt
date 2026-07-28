package com.androidvisualqa.testing

import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.model.ids.ReportId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

/**
 * Creates a minimal [VisualFeedbackReport] for use in tests.
 *
 * ponytail: only required fields are set; add factory overrides as the schema grows.
 */
public fun testReport(
    reportId: ReportId = ReportId(UUID.randomUUID().toString()),
    createdAt: Instant = Clock.System.now(),
    capture: com.androidvisualqa.model.capture.CaptureSession = com.androidvisualqa.model.capture.CaptureSession(
        sessionId = reportId.value,
        startedAt = createdAt,
        triggerSource = com.androidvisualqa.model.capture.TriggerSource.AccessibilityOverlay,
        captureMode = com.androidvisualqa.model.capture.CaptureMode.Still,
        lastStateName = "Complete",
    ),
    frame: com.androidvisualqa.model.capture.CaptureFrame = com.androidvisualqa.model.capture.CaptureFrame(
        displayId = 0,
        windowId = 42,
        packageName = "com.example.app",
        widthPx = 1080,
        heightPx = 2400,
        density = 2.0f,
        rotationDegrees = 0,
        screenshotMethod = com.androidvisualqa.model.capture.ScreenshotMethod.AccessibilityWindow,
        monotonicTimestamp = 0L,
        wallClockTimestamp = createdAt,
    ),
): VisualFeedbackReport = VisualFeedbackReport(
    reportId = reportId,
    createdAt = createdAt,
    status = com.androidvisualqa.model.ReportStatus.Draft,
    capture = capture,
    frame = frame,
    feedback = com.androidvisualqa.model.feedback.FeedbackEvidence(),
)
