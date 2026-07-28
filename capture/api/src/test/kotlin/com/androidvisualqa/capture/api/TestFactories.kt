package com.androidvisualqa.capture.api

import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import com.androidvisualqa.geometry.Rotation
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Factory functions with sensible defaults for capture-api test fixtures.
 */

fun testContextSnapshot(
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

fun testCapturedFrame(
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
