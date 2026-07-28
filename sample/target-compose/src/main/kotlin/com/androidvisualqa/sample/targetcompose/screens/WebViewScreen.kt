package com.androidvisualqa.sample.targetcompose.screens

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import com.androidvisualqa.sdk.compose.feedbackTarget

/**
 * Screen with an [AndroidView] hosting a [WebView].
 *
 * The WebView loads [assets/sample.html] — a static HTML file with buttons
 * and text inputs.
 *
 * **Known limitation:** The SDK's [feedbackTarget] is placed on the wrapping
 * [AndroidView] container only. The WebView's DOM children (HTML buttons,
 * inputs, etc.) are opaque to the Compose semantics tree and cannot be
 * individually targeted. The matching engine sees one SDK component
 * ("webview.container") with the full WebView bounds.
 *
 * This screen exists to verify that the SDK correctly marks the WebView
 * container and does not crash when a non-Compose view is embedded.
 */
class WebViewScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WebViewScreen() }
    }
}

@Composable
fun WebViewScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "WebView Screen",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = false
                    loadUrl("file:///android_asset/sample.html")
                }
            },
            modifier = Modifier
                .feedbackTarget(stableId = "webview.container")
                .fillMaxSize()
                .height(400.dp),
        )
    }
}
