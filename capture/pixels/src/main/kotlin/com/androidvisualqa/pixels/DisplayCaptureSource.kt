package com.androidvisualqa.pixels

/**
 * Source of display-level screenshots (API 30–33 fallback).
 *
 * Falls back to `AccessibilityService.takeScreenshotOfDisplay` when
 * window-level capture is unavailable, the window ID is unknown, or
 * window-level capture fails.
 */
public fun interface DisplayCaptureSource {
    public suspend fun captureDisplay(displayId: Int): PixelCaptureResult
}
