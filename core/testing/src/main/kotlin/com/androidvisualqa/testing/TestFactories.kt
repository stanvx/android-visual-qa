package com.androidvisualqa.testing

import com.androidvisualqa.capture.api.CapturedFrame
import com.androidvisualqa.capture.api.ContextSnapshot
import com.androidvisualqa.core.geometry.Bounds
import com.androidvisualqa.core.geometry.Rotation
import com.androidvisualqa.core.geometry.ScreenPx
import com.androidvisualqa.core.model.VisualFeedbackReport
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
    bounds: Bounds<ScreenPx> = Bounds(ScreenPx(0f), ScreenPx(0f), ScreenPx(1080f), ScreenPx(2400f)),
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
    rotation: Rotation = Rotation.Rotation_0,
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
    reportId: String = UUID.randomUUID().toString(),
    createdAt: Instant = Clock.System.now(),
): VisualFeedbackReport = VisualFeedbackReport(
    schemaVersion = 1,
    reportId = reportId,
    createdAt = createdAt,
    status = com.androidvisualqa.core.model.ReportStatus.Draft,
    // ponytail: remaining fields use their defaults from the model definition.
    // Add overrides when tests require specific capture/annotation/feedback data.
)
