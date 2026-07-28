package com.androidvisualqa.sdk.composecore

import kotlinx.serialization.Serializable

/**
 * Build metadata for the host application.
 *
 * Pure data — no Android [BuildConfig] dependency.
 *
 * @property buildType The Gradle build type (e.g. "debug", "release").
 * @property buildId An optional CI or build-system identifier.
 * @property gitSha The commit SHA of the host app at build time.
 * @property isDebuggable Whether the host app was built in debuggable mode.
 */
@Serializable
data class BuildMetadata(
    val buildType: String,
    val buildId: String? = null,
    val gitSha: String? = null,
    val isDebuggable: Boolean = false,
)
