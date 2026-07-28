package com.androidvisualqa.pixels

import com.androidvisualqa.capture.api.CaptureFailure

/**
 * Outcome of a pixel capture attempt.
 *
 * @see PixelCaptureSource
 * @see CaptureFailure
 */
public sealed interface PixelCaptureResult {

    /** The capture succeeded and pixel data is available. */
    public data class Success(val pixels: CapturedPixels) : PixelCaptureResult

    /** The capture failed with a typed [CaptureFailure] reason. */
    public data class Failure(val reason: CaptureFailure) : PixelCaptureResult
}
