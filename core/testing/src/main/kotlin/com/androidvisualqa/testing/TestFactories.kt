package com.androidvisualqa.testing

import com.androidvisualqa.capture.api.CapturedFrame
import com.androidvisualqa.capture.api.ContextSnapshot
import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import com.androidvisualqa.geometry.Rotation
import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.model.ids.ReportId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

/**
 * Factory functions with sensible defaults for test fixtures.
 *
 * All functions produce deterministic values when no arguments are overridden.
 */

public fun testContextSnapshot(
    packageName: String = "com.example.app",
    windowId: Long? = 42L,
    displayId: Int = 0,
    bounds: Bounds<CoordinateSpace.ScreenPx> = Bounds(
        left = 0.0, top = 0.0,
        right = 1080.0, bottom = 2400.0,
        space = CoordinateSpace.ScreenPx,
    ),
    capturedAt: Instant = Clock.System.now(),
): ContextSnapshot = ContextSnapshot(
    packageName = packageName,
    windowId = windowId,
    displayId = displayId,
    bounds = bounds,
    capturedAt = capturedAt,
)

public fun testCapturedFrame(
    displayId: Int = 0,
    widthPx: Int = 1080,
    heightPx: Int = 2400,
    rotation: Rotation = Rotation.ROTATION_0,
    capturedAt: Instant = Clock.System.now(),
): CapturedFrame = CapturedFrame(
    displayId = displayId,
    widthPx = widthPx,
    heightPx = heightPx,
    rotation = rotation,
    capturedAt = capturedAt,
)

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
        state = com.androidvisualqa.model.capture.SessionState.Complete,
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
