package com.androidvisualqa.sdk.composecore

import kotlinx.serialization.Serializable

/**
 * Pure-Kotlin description of a composable component that the SDK can register.
 *
 * @property stableId Developer-provided stable key (e.g. "checkout.payment.continue").
 * @property route The current navigation route, if available.
 * @property designSystem Design-system tokens captured from the component.
 * @property privacy Privacy classification for this component.
 * @property build Build metadata for the host application.
 * @property semantics Arbitrary key-value metadata (e.g. "variant" -> "large").
 * @property boundsLeft Left edge of the component bounds in screen pixels. 0 if not yet laid out.
 * @property boundsTop Top edge of the component bounds in screen pixels. 0 if not yet laid out.
 * @property boundsRight Right edge of the component bounds in screen pixels. 0 if not yet laid out.
 * @property boundsBottom Bottom edge of the component bounds in screen pixels. 0 if not yet laid out.
 */
@Serializable
data class SdkComponentDescriptor(
    val stableId: String,
    val route: SdkRoute? = null,
    val designSystem: DesignSystemSnapshot? = null,
    val privacy: PrivacyClassification? = null,
    val build: BuildMetadata? = null,
    val semantics: Map<String, String> = emptyMap(),
    val boundsLeft: Int = 0,
    val boundsTop: Int = 0,
    val boundsRight: Int = 0,
    val boundsBottom: Int = 0,
)
