package com.androidvisualqa.sdk.compose

import android.content.pm.PackageInfo
import android.os.Build
import com.androidvisualqa.model.capture.FeedbackPrivacy
import com.androidvisualqa.model.capture.SdkComponentSnapshot
import com.androidvisualqa.model.ids.SdkComponentId
import com.androidvisualqa.privacy.Sensitivity
import com.androidvisualqa.sdk.composecore.BuildMetadata
import com.androidvisualqa.sdk.composecore.DesignSystemSnapshot
import com.androidvisualqa.sdk.composecore.SdkEvidence
import com.androidvisualqa.sdk.composecore.SdkEvidenceProducer
import com.androidvisualqa.sdk.composecore.SdkRegistry

/**
 * Android-side implementation of [SdkEvidenceProducer].
 *
 * Produces [SdkEvidence] by combining:
 * - All currently registered component descriptors from the [SdkRegistry].
 * - Build metadata derived from the host application's [PackageInfo] and
 *   the SDK's own [BuildConfig] fields.
 *
 * Bounds are read from the registry descriptors (captured at layout time by
 * [Modifier.feedbackTarget]) rather than computed here.
 *
 * @param registry The [SdkRegistry] containing registered component descriptors.
 * @param packageInfo The [PackageInfo] for the host application.
 * @param isDebuggable Whether the host app was built in debuggable mode.
 * @param buildType The Gradle build type string.
 * @param gitSha The commit SHA of the host app at build time.
 */
class AndroidSdkEvidenceProducer(
    private val registry: SdkRegistry,
    private val packageInfo: PackageInfo,
    private val isDebuggable: Boolean,
    private val buildType: String,
    private val gitSha: String? = null,
) : SdkEvidenceProducer {

    override suspend fun snapshot(): SdkEvidence {
        val descriptors = registry.all()

        val components = descriptors.map { descriptor ->
            SdkComponentSnapshot(
                componentId = SdkComponentId(descriptor.stableId),
                componentType = "sdk.component",
                route = descriptor.route?.path,
                boundsLeft = 0,
                boundsTop = 0,
                boundsRight = 0,
                boundsBottom = 0,
                role = descriptor.stableId,
                state = null,
                testTag = null,
                designToken = descriptor.designSystem?.tokens?.firstOrNull()?.value,
                variant = descriptor.semantics["variant"],
                sourceHint = "sdk",
                appVersionName = packageInfo.versionName
                    ?: packageInfo.longVersionCode.toString(),
                appVersionCode = if (Build.VERSION.SDK_INT >= 28) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                },
                appCommitSha = gitSha,
                appBuildType = buildType,
                privacyClassification = descriptor.privacy?.let { mapPrivacy(it) },
            )
        }

        return SdkEvidence(
            components = components,
            routes = descriptors.mapNotNull { it.route }.distinctBy { it.path },
            designSystemSnapshots = descriptors
                .mapNotNull { descriptor ->
                    descriptor.designSystem?.let { descriptor.stableId to it }
                }
                .toMap(),
            privacy = descriptors
                .mapNotNull { descriptor ->
                    descriptor.privacy?.let { descriptor.stableId to it }
                }
                .toMap(),
            build = BuildMetadata(
                buildType = buildType,
                isDebuggable = isDebuggable,
                gitSha = gitSha,
            ),
        )
    }

    /**
     * Map [Sensitivity] to the schema's [FeedbackPrivacy].
     *
     * The SDK schema has three values: [FeedbackPrivacy.Public], [FeedbackPrivacy.Sensitive],
     * and [FeedbackPrivacy.NeverCapture]. The privacy module has a richer enum. This
     * mapping decides the conservative bucket:
     * - [Sensitivity.Public] → [FeedbackPrivacy.Public]
     * - Everything else    → [FeedbackPrivacy.Sensitive]
     */
    private fun mapPrivacy(sensitivity: Sensitivity): FeedbackPrivacy {
        return when (sensitivity) {
            Sensitivity.Public -> FeedbackPrivacy.Public
            else -> FeedbackPrivacy.Sensitive
        }
    }
}
