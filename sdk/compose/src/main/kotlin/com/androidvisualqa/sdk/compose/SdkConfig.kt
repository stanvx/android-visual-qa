package com.androidvisualqa.sdk.compose

/**
 * Configuration for [VisualFeedbackHost].
 *
 * @property autoRegister If true, the host provides the [com.androidvisualqa.sdk.composecore.SdkRegistry]
 *   to descendants via [androidx.compose.runtime.CompositionLocal]. Consumers still need to
 *   call [Modifier.feedbackTarget] on each component — this flag controls whether the registry
 *   is wired at all (the host itself does not auto-register arbitrary composables).
 * @property debugLogging If true, the host emits debug-level logcat messages.
 */
data class SdkConfig(
    val autoRegister: Boolean = true,
    val debugLogging: Boolean = false,
)
