package com.androidvisualqa.pixels

import com.androidvisualqa.geometry.Rotation
import kotlinx.datetime.Instant
import java.util.Objects

/**
 * The raw pixel data and metadata from a successful capture.
 *
 * @property displayId The display this was captured from.
 * @property widthPx Image width in pixels.
 * @property heightPx Image height in pixels.
 * @property rotation Display rotation at capture time.
 * @property rgba8888 Raw RGBA 8888 pixel data (4 bytes per pixel, row-major).
 * @property capturedAt Wall-clock timestamp of the capture.
 */
public data class CapturedPixels(
    val displayId: Int,
    val widthPx: Int,
    val heightPx: Int,
    val rotation: Rotation,
    public val rgba8888: ByteArray,
    val capturedAt: Instant,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CapturedPixels) return false
        return displayId == other.displayId &&
            widthPx == other.widthPx &&
            heightPx == other.heightPx &&
            rotation == other.rotation &&
            rgba8888.contentEquals(other.rgba8888) &&
            capturedAt == other.capturedAt
    }

    override fun hashCode(): Int = Objects.hash(
        displayId, widthPx, heightPx, rotation, rgba8888.contentHashCode(), capturedAt,
    )
}
