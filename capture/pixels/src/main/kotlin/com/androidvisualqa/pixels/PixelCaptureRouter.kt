package com.androidvisualqa.pixels

import com.androidvisualqa.capture.api.CaptureFailure

/**
 * Orchestrates the pixel capture fallback chain.
 *
 * Attempt order:
 * 1. Window-level capture via [windowSource] (if available and a [PixelCaptureRequest.windowId] is set).
 * 2. Display-level capture via [displaySource].
 * 3. If both fail, return the most specific failure.
 *
 * @param windowSource Window-level capture source (nullable — null if not available on the device).
 * @param displaySource Display-level capture source (required fallback).
 */
public class PixelCaptureRouter(
    private val windowSource: PixelCaptureSource?,
    private val displaySource: DisplayCaptureSource,
) {
    /**
     * Attempts to capture the requested pixels through the fallback chain.
     *
     * 1. If [windowSource] is non-null and [request.windowId] is non-null, try window capture.
     *    - On [CaptureFailure.WindowUnavailable] or any other failure, fall through to display.
     * 2. Try display capture.
     * 3. If both failed, return the window failure if one occurred, otherwise the display failure.
     */
    public suspend fun capture(request: PixelCaptureRequest): PixelCaptureResult {
        // Stage 1: window-level capture
        val windowResult: PixelCaptureResult? = if (windowSource != null && request.windowId != null) {
            windowSource.capture(request)
        } else {
            null
        }

        // If the window capture succeeded, return it immediately
        if (windowResult is PixelCaptureResult.Success) {
            return windowResult
        }

        // Stage 2: display-level capture (fallback)
        val displayResult = displaySource.captureDisplay(request.displayId)
        if (displayResult is PixelCaptureResult.Success) {
            return displayResult
        }

        // Both failed — prefer the window failure (more specific) over display failure
        return when (windowResult) {
            is PixelCaptureResult.Failure -> windowResult
            else -> displayResult // already a Failure
        }
    }
}
