package com.androidvisualqa.model.capture

import com.androidvisualqa.model.ids.SdkComponentId
import kotlinx.serialization.Serializable

/**
 * A component snapshot provided by the Compose enrichment SDK.
 *
 * Contains first-party metadata not available from the accessibility tree alone.
 *
 * @property componentId Stable developer-provided key.
 * @property componentType Semantic component type.
 * @property route Screen or navigation route ID.
 * @property boundsLeft Exact on-screen bounds (pixels).
 * @property boundsTop Exact on-screen bounds (pixels).
 * @property boundsRight Exact on-screen bounds (pixels).
 * @property boundsBottom Exact on-screen bounds (pixels).
 * @property role Semantic role label.
 * @property state Component state string.
 * @property testTag Developer-assigned test tag.
 * @property designToken Design-system token or style reference.
 * @property variant Component variant name.
 * @property sourceHint Optional breadcrumb from the host app.
 * @property appVersionName Build metadata: version name.
 * @property appVersionCode Build metadata: version code (Long).
 * @property appCommitSha Build metadata: commit SHA.
 * @property appBuildType Build metadata: debug/release.
 * @property privacyClassification Sensitive, public, or neverCapture.
 */
@Serializable
data class SdkComponentSnapshot(
    val componentId: SdkComponentId,
    val componentType: String,
    val route: String? = null,
    val boundsLeft: Int = 0,
    val boundsTop: Int = 0,
    val boundsRight: Int = 0,
    val boundsBottom: Int = 0,
    val role: String? = null,
    val state: String? = null,
    val testTag: String? = null,
    val designToken: String? = null,
    val variant: String? = null,
    val sourceHint: String? = null,
    val appVersionName: String? = null,
    val appVersionCode: Long? = null,
    val appCommitSha: String? = null,
    val appBuildType: String? = null,
    val privacyClassification: FeedbackPrivacy? = null,
)

@Serializable
enum class FeedbackPrivacy {
    Public,
    Sensitive,
    NeverCapture,
}
