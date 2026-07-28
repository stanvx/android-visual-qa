package com.androidvisualqa.capture.api

import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import kotlinx.datetime.Instant

/**
 * Immutable snapshot of the target window's context at a point-in-time.
 *
 * @property packageName Package name of the captured app.
 * @property windowId Accessibility window ID (0 if unknown).
 * @property displayId Logical Android display ID.
 * @property bounds Window bounds in screen pixels.
 * @property capturedAt Wall-clock timestamp of the snapshot.
 */
public data class ContextSnapshot(
    val packageName: String,
    val windowId: Long?,
    val displayId: Int,
    val bounds: Bounds<CoordinateSpace.ScreenPx>,
    val capturedAt: Instant,
)
