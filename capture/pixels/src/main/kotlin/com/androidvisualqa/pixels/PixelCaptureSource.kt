package com.androidvisualqa.pixels

/**
 * Source of pixel-level window screenshots.
 *
 * **Real implementations live in M1 lane H-fix** (the Android-side adapter module).
 * This interface is the pure-Kotlin seam for testability and compile-time wiring.
 *
 * A [PixelCaptureSource] captures a specific window identified by
 * [PixelCaptureRequest.windowId] using the platform-specific screenshot API
 * (e.g. `AccessibilityService.takeScreenshotOfWindow` on API 34+).
 */
public fun interface PixelCaptureSource {
    public suspend fun capture(request: PixelCaptureRequest): PixelCaptureResult
}
