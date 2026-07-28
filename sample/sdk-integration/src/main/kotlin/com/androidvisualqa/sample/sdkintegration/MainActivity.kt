package com.androidvisualqa.sample.sdkintegration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.androidvisualqa.sdk.compose.SdkConfig
import com.androidvisualqa.sdk.compose.VisualFeedbackHost

/**
 * Main entry point for the SDK integration sample.
 *
 * Wraps the app content in [VisualFeedbackHost], which provides the
 * [com.androidvisualqa.sdk.composecore.SdkRegistry] to descendant composables
 * via [androidx.compose.runtime.CompositionLocal].
 *
 * The [SdkConfig.debugLogging] flag is set to `true` here for demonstration;
 * production apps should omit it or read it from BuildConfig.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VisualFeedbackHost(
                config = SdkConfig(debugLogging = true),
            ) {
                FeedbackSampleScreen()
            }
        }
    }
}
