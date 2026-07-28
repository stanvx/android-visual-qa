package com.androidvisualqa.capture.api

import com.androidvisualqa.core.geometry.Bounds
import com.androidvisualqa.core.geometry.ScreenPx
import kotlinx.datetime.Instant

/**
 * Immutable snapshot of the target window's context at a point-in-time.
 *
 * Depends on [Bounds] and [ScreenPx] from `:core:geometry`.
 */
public data class ContextSnapshot(
    val packageName: String,
    val windowId: Long?,
    val displayId: Int,
    val bounds: Bounds<ScreenPx>,
    val capturedAt: Instant,
)
