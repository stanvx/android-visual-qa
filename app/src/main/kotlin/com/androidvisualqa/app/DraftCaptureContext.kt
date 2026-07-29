package com.androidvisualqa.app

import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import com.androidvisualqa.model.capture.CaptureFrame
import com.androidvisualqa.model.capture.CaptureSession
import com.androidvisualqa.model.capture.NodeSnapshot
import kotlinx.serialization.Serializable

/** Persisted evidence needed to finish a captured draft after process death. */
@Serializable
public data class DraftCaptureContext(
    val frame: CaptureFrame,
    val session: CaptureSession,
    val candidates: List<NodeSnapshot>,
    val screenLeft: Double,
    val screenTop: Double,
    val screenRight: Double,
    val screenBottom: Double,
) {
    public fun screenBounds(): Bounds<CoordinateSpace.ScreenPx> = Bounds(
        left = screenLeft,
        top = screenTop,
        right = screenRight,
        bottom = screenBottom,
        space = CoordinateSpace.ScreenPx,
    )
}
