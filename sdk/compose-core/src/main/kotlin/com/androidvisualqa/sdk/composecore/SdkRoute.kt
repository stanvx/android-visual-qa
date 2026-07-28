package com.androidvisualqa.sdk.composecore

import kotlinx.serialization.Serializable

/**
 * A serializable representation of a Compose navigation route.
 *
 * @property path The route path (e.g. "checkout/payment").
 * @property arguments Key-value route arguments, if any.
 */
@Serializable
data class SdkRoute(
    val path: String,
    val arguments: Map<String, String> = emptyMap(),
)
