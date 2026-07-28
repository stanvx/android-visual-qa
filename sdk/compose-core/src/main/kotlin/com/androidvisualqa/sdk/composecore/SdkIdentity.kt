package com.androidvisualqa.sdk.composecore

/**
 * Wire-stable schema version for SDK-side payloads.
 *
 * Matches [com.androidvisualqa.model.VisualFeedbackReport.CURRENT_SCHEMA_VERSION]
 * when the SDK produces reports directly.
 */
object SdkIdentity {
    const val SCHEMA_VERSION = 1
}
