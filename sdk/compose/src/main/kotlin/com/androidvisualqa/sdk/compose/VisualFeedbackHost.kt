package com.androidvisualqa.sdk.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.androidvisualqa.sdk.composecore.InMemorySdkRegistry
import com.androidvisualqa.sdk.composecore.SdkRegistry

/**
 * Top-level Compose host that wires the SDK's [SdkRegistry] into the composition tree.
 *
 * Wrap this around your app's root content to enable [Modifier.feedbackTarget] on
 * descendant composables:
 *
 * ```kotlin
 * setContent {
 *     VisualFeedbackHost {
 *         App()
 *     }
 * }
 * ```
 *
 * The host provides the [LocalSdkRegistry] [androidx.compose.runtime.CompositionLocal]
 * so that [Modifier.feedbackTarget] can register/unregister descriptors on behalf of
 * individual composables.
 *
 * @param modifier Modifier for the host's root layout.
 * @param config SDK configuration.
 * @param content The child composable tree.
 */
@Composable
fun VisualFeedbackHost(
    modifier: Modifier = Modifier,
    config: SdkConfig = SdkConfig(),
    content: @Composable () -> Unit,
) {
    val registry = remember { InMemorySdkRegistry() }

    CompositionLocalProvider(LocalSdkRegistry provides registry) {
        content()
    }
}
