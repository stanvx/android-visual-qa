package com.androidvisualqa.geometry

/**
 * Immutable snapshot of the physical display characteristics at capture time.
 *
 * @property density  Screen density factor (logical density, e.g. 2.0 for
 *                    mdpi→xhdpi, 3.0 for xxhdpi).
 * @property widthPx  Display width in raw pixels.
 * @property heightPx Display height in raw pixels.
 * @property rotation Surface rotation constant (Surface.ROTATION_0/90/180/270).
 */
data class DisplayMetrics(
    val density: Double,
    val widthPx: Int,
    val heightPx: Int,
    val rotation: Int,
) {
    /** The current [Rotation] enum value derived from [rotation]. */
    val rotationEnum: Rotation
        get() = when (rotation) {
            0 -> Rotation.ROTATION_0
            1 -> Rotation.ROTATION_90
            2 -> Rotation.ROTATION_180
            3 -> Rotation.ROTATION_270
            else -> Rotation.ROTATION_0
        }
}
