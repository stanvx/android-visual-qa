package com.androidvisualqa.sample.targetcompose.screens

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.androidvisualqa.sdk.compose.feedbackTarget

/**
 * Screen with [WindowManager.LayoutParams.FLAG_SECURE] set.
 *
 * The activity registers with `android:taskAffinity=""` and
 * `android:excludeFromRecents="true"` in the manifest.
 *
 * Used to verify the secure-window classifier: the SDK must detect
 * FLAG_SECURE and report the component's privacy classification
 * accordingly, preventing screenshots on this activity.
 */
class SecureScreenScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent { SecureScreenScreen() }
    }
}

@Composable
fun SecureScreenScreen() {
    Column(
        modifier = Modifier
            .feedbackTarget(stableId = "secure.screen.root")
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "FLAG_SECURE Screen",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(
            text = "This activity has FLAG_SECURE enabled. " +
                    "Screenshots and screen recording are blocked by the system. " +
                    "The SDK should classify this as sensitive/never-capture.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
