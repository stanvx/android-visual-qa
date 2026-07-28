package com.androidvisualqa.sdk.composecore

import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace

/**
 * An SDK-produced override used by the matching engine's [SdkOverrideApplier].
 *
 * @property stableId The stable component identifier.
 * @property bounds Exact on-screen bounds of the component.
 * @property sensitivity The privacy classification for this component.
 */
data class SdkOverride(
    val stableId: String,
    val bounds: Bounds<CoordinateSpace.ScreenPx>,
    val sensitivity: SdkSensitivity,
)
