package com.androidvisualqa.sdk.compose

/**
 * Configuration for [VisualFeedbackHost].
 *
 * @property debugLogging If true, the host emits debug-level logcat messages.
 *
 * // TODO(m5): add autoRegister opt-out when the registry can be externally provided
 */
data class SdkConfig(
    val debugLogging: Boolean = false,
)
