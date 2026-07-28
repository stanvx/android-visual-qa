package com.androidvisualqa.model.capture

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * A single captured frame — screenshot plus display/window metadata.
 *
 * Geometry types (bounds, density, rotation) are plain primitives in M0.
 *
 * @property displayId Logical Android display ID.
 * @property windowId Accessibility window ID (0 if unknown).
 * @property packageName Package of the captured app.
 * @property activityTitle Optional activity or window title.
 * @property widthPx Pixel width of the captured image.
 * @property heightPx Pixel height of the captured image.
 * @property density Display density (DPI).
 * @property rotationDegrees Display rotation in degrees (0, 90, 180, 270).
 * @property systemBarsTop Insets from the top system bar (pixels).
 * @property systemBarsBottom Insets from the bottom system bar (pixels).
 * @property systemBarsLeft Insets from the left system bar (pixels).
 * @property systemBarsRight Insets from the right system bar (pixels).
 * @property windowBoundsLeft Window bounds on screen (pixels).
 * @property windowBoundsTop Window bounds on screen (pixels).
 * @property windowBoundsRight Window bounds on screen (pixels).
 * @property windowBoundsBottom Window bounds on screen (pixels).
 * @property contentBoundsLeft Captured content bounds (pixels).
 * @property contentBoundsTop Captured content bounds (pixels).
 * @property contentBoundsRight Captured content bounds (pixels).
 * @property contentBoundsBottom Captured content bounds (pixels).
 * @property screenshotMethod How the pixels were obtained.
 * @property monotonicTimestamp Nanosecond monotonic clock value.
 * @property wallClockTimestamp Wall-clock timestamp of capture.
 */
@Serializable
data class CaptureFrame(
    val displayId: Int,
    val windowId: Int,
    val packageName: String,
    val activityTitle: String? = null,
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    val rotationDegrees: Int,
    val systemBarsTop: Int = 0,
    val systemBarsBottom: Int = 0,
    val systemBarsLeft: Int = 0,
    val systemBarsRight: Int = 0,
    val windowBoundsLeft: Int = 0,
    val windowBoundsTop: Int = 0,
    val windowBoundsRight: Int = 0,
    val windowBoundsBottom: Int = 0,
    val contentBoundsLeft: Int = 0,
    val contentBoundsTop: Int = 0,
    val contentBoundsRight: Int = 0,
    val contentBoundsBottom: Int = 0,
    val screenshotMethod: ScreenshotMethod,
    val monotonicTimestamp: Long,
    val wallClockTimestamp: Instant,
)

@Serializable
enum class ScreenshotMethod {
    AccessibilityWindow,
    AccessibilityDisplay,
    MediaProjection,
    ManualImport,
}
