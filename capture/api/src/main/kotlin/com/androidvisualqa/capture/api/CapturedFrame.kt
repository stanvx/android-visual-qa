package com.androidvisualqa.capture.api

import com.androidvisualqa.geometry.Rotation
import kotlinx.datetime.Instant

/**
 * Metadata for a successfully captured pixel frame.
 *
 * @property displayId Logical Android display ID.
 * @property widthPx Pixel width of the captured image.
 * @property heightPx Pixel height of the captured image.
 * @property rotation Display rotation when captured.
 * @property capturedAt Wall-clock timestamp of capture.
 */
public data class CapturedFrame(
    val displayId: Int,
    val widthPx: Int,
    val heightPx: Int,
    val rotation: Rotation,
    val capturedAt: Instant,
    /** PNG bytes for the frozen frame, when the capture source provides them. */
    val pngBytes: ByteArray = byteArrayOf(),
)
