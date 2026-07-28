package com.androidvisualqa.sdk.compose

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntRect
import com.androidvisualqa.sdk.composecore.BuildMetadata
import com.androidvisualqa.sdk.composecore.DesignSystemSnapshot
import com.androidvisualqa.sdk.composecore.PrivacyClassification
import com.androidvisualqa.sdk.composecore.SdkComponentDescriptor
import com.androidvisualqa.sdk.composecore.SdkRoute

/**
 * Mark a Composable as a visual-feedback SDK target.
 *
 * The target is registered with the nearest [VisualFeedbackHost]'s SdkRegistry
 * on first composition and unregistered on dispose. Bounds are captured via
 * [onGloballyPositioned] after the first layout pass and stored in screen-pixel
 * coordinates (not local coordinates).
 *
 * **SDK contract:** Each consumer is responsible for calling [Modifier.feedbackTarget]
 * on every Composable they want to appear in the SDK evidence. There is no
 * automatic registration of all Composables in the tree.
 *
 * Example:
 * ```kotlin
 * Button(
 *     modifier = Modifier.feedbackTarget(
 *         stableId = "checkout.payment.continue",
 *         route = SdkRoute("checkout/payment"),
 *         semantics = mapOf("variant" to "primary", "size" to "large"),
 *     ),
 *     onClick = ::handleContinue,
 * ) { Text("Continue") }
 * ```
 *
 * @param stableId A developer-provided stable identifier (e.g. "checkout.payment.continue").
 *   Must be unique within the current composition tree.
 * @param route The current navigation route, if available.
 * @param designSystem Design-system tokens captured from the component.
 * @param privacy Privacy classification for this component.
 * @param semantics Arbitrary key-value metadata (e.g. "variant" -> "large").
 * @param build Build metadata for the host application.
 */
fun Modifier.feedbackTarget(
    stableId: String,
    route: SdkRoute? = null,
    designSystem: DesignSystemSnapshot? = null,
    privacy: PrivacyClassification? = null,
    semantics: Map<String, String> = emptyMap(),
    build: BuildMetadata? = null,
): Modifier = composed {
    val registry = LocalSdkRegistry.current

    DisposableEffect(stableId) {
        val descriptor = SdkComponentDescriptor(
            stableId = stableId,
            route = route,
            designSystem = designSystem,
            privacy = privacy,
            build = build,
            semantics = semantics,
        )
        registry.register(descriptor)
        onDispose {
            registry.unregister(stableId)
        }
    }

    onGloballyPositioned { coordinates ->
        // Capture bounds after first layout. Compose may emit a zero-bounds frame
        // during initial layout — skip that to avoid registering garbage bounds.
        // ponytail: initial-frame zero-bounds race. If a component's position changes
        // after first layout (e.g. animation), this will update the bounds. If
        // multiple rapid relayouts cause performance issues, switch to a
        // onGloballyPositioned-throttle or use snapshotFlow + coordinates.
        val layoutBounds = coordinates.boundsInWindow()
        if (layoutBounds.width > 0 && layoutBounds.height > 0) {
            val updated = SdkComponentDescriptor(
                stableId = stableId,
                route = route,
                designSystem = designSystem,
                privacy = privacy,
                build = build,
                semantics = semantics,
                boundsLeft = layoutBounds.left.toInt(),
                boundsTop = layoutBounds.top.toInt(),
                boundsRight = layoutBounds.right.toInt(),
                boundsBottom = layoutBounds.bottom.toInt(),
            )
            registry.update(stableId, updated)
        }
    }
}
