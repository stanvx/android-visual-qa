package com.androidvisualqa.sample.targetcompose.screens

import android.os.Bundle
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
 * Screen supporting multi-window mode.
 *
 * The activity declares `android:resizeableActivity="true"` and
 * `android:supportsPictureInPicture="false"` in the manifest.
 *
 * Used to verify that the SDK correctly reports bounds when the
 * activity is in split-screen or freeform mode (reduced window size).
 */
class SplitScreenScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SplitScreenScreen() }
    }
}

@Composable
fun SplitScreenScreen() {
    Column(
        modifier = Modifier
            .feedbackTarget(stableId = "splitscreen.screen.root")
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Split-Screen Demo",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Text(
            text = "This activity supports multi-window mode. " +
                    "Try entering split-screen from the overview menu.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .feedbackTarget(stableId = "splitscreen.description")
                .padding(bottom = 8.dp),
        )

        Text(
            text = "Bounds should update on window resize.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.feedbackTarget(stableId = "splitscreen.hint"),
        )
    }
}
