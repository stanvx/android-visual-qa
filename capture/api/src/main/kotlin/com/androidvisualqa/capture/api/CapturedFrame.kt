package com.androidvisualqa.capture.api

import com.androidvisualqa.core.geometry.Rotation
import kotlinx.datetime.Instant

/**
 * Metadata for a captured pixel frame (screenshot).
 *
 * Depends on [Rotation] from `:core:geometry`.
 */
public data class CapturedFrame(
    val displayId: Int,
    val widthPx: Int,
    val heightPx: Int,
    val rotation: Rotation,
    val capturedAt: Instant,
)
