package com.androidvisualqa.sdk.compose

import androidx.compose.runtime.compositionLocalOf
import com.androidvisualqa.sdk.composecore.InMemorySdkRegistry
import com.androidvisualqa.sdk.composecore.SdkRegistry

/**
 * [androidx.compose.runtime.CompositionLocal] carrying the [SdkRegistry] for the current
 * [VisualFeedbackHost] scope.
 *
 * Falls back to a no-op [NoopSdkRegistry] when no host is present in the tree, so
 * [Modifier.feedbackTarget] is safe to use without a host (it simply won't register).
 */
val LocalSdkRegistry = compositionLocalOf { NoopSdkRegistry() as SdkRegistry }

/**
 * A no-op [SdkRegistry] used as the default value of [LocalSdkRegistry] when no
 * [VisualFeedbackHost] is present in the composition tree.
 *
 * This prevents crashes in test environments or partial migrations where the
 * SDK is not fully wired.
 */
internal class NoopSdkRegistry : SdkRegistry {
    override fun register(descriptor: com.androidvisualqa.sdk.composecore.SdkComponentDescriptor): Boolean = false
    override fun unregister(stableId: String): Boolean = false
    override fun get(stableId: String): com.androidvisualqa.sdk.composecore.SdkComponentDescriptor? = null
    override fun all(): List<com.androidvisualqa.sdk.composecore.SdkComponentDescriptor> = emptyList()
}
