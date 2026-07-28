package com.androidvisualqa.geometry

/**
 * Display rotation as an enum, matching `Surface.ROTATION_*` constants.
 */
enum class Rotation(
    /** The surface rotation integer (0, 90, 180, 270). */
    val surfaceRotation: Int,
) {
    ROTATION_0(0),
    ROTATION_90(90),
    ROTATION_180(180),
    ROTATION_270(270);

    /** True when the display is in a landscape orientation. */
    val isLandscape: Boolean get() = this == ROTATION_90 || this == ROTATION_270

    companion object {
        /**
         * Maps a `Surface.ROTATION_*` integer (0–3) to the corresponding [Rotation].
         *
         * Values outside 0–3 return [ROTATION_0] as a safe default.
         */
        fun fromSurfaceRotation(value: Int): Rotation = when (value) {
            0 -> ROTATION_0
            1 -> ROTATION_90
            2 -> ROTATION_180
            3 -> ROTATION_270
            else -> ROTATION_0
        }
    }
}
