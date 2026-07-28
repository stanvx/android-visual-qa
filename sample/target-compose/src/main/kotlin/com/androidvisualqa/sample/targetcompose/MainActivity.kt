package com.androidvisualqa.sample.targetcompose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.androidvisualqa.sample.targetcompose.screens.CanvasScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.DialogScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.ImeScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.LazyColumnScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.MergedSemanticsScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.NestedClickTargetsScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.PasswordFieldScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.RotationScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.SecureScreenScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.SplitScreenScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.TabletLayoutScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.WebViewScreenActivity

/**
 * Entry point for the deterministic Compose test-fixture app.
 *
 * Renders a [LazyColumn] menu of all edge-case screens. Each item is a tappable
 * link that opens the corresponding [ComponentActivity].
 *
 * Used by the E2E harness (UiAutomator) for device-verification runs.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen(
                    onNavigate = { activityClass ->
                        startActivity(Intent(this@MainActivity, activityClass))
                    },
                )
            }
        }
    }
}

private data class MenuItem(
    val label: String,
    val description: String,
    val activity: Class<out ComponentActivity>,
)

@Composable
private fun MainScreen(
    onNavigate: (Class<out ComponentActivity>) -> Unit,
) {
    val items = listOf(
        MenuItem("Merged Semantics", "Card with merged icon+text semantics", MergedSemanticsScreenActivity::class.java),
        MenuItem("Nested Click Targets", "Outer card, inner button, innermost image button", NestedClickTargetsScreenActivity::class.java),
        MenuItem("LazyColumn", "1000-item list with mixed types", LazyColumnScreenActivity::class.java),
        MenuItem("WebView", "AndroidView hosting a WebView", WebViewScreenActivity::class.java),
        MenuItem("Canvas", "Custom drawn rectangles and labels", CanvasScreenActivity::class.java),
        MenuItem("Password Field", "OutlinedTextField with password visual transformation", PasswordFieldScreenActivity::class.java),
        MenuItem("Dialog", "AlertDialog on button tap", DialogScreenActivity::class.java),
        MenuItem("IME", "TextField + submit button for keyboard insets", ImeScreenActivity::class.java),
        MenuItem("Secure Screen", "FLAG_SECURE activity", SecureScreenScreenActivity::class.java),
        MenuItem("Rotation", "Portrait vs landscape layout switch", RotationScreenActivity::class.java),
        MenuItem("Split Screen", "Resizeable multi-window support", SplitScreenScreenActivity::class.java),
        MenuItem("Tablet Layout", "WindowSizeClass: compact / medium / expanded", TabletLayoutScreenActivity::class.java),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        items(items.size) { index ->
            val item = items[index]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onNavigate(item.activity) }
                    .padding(vertical = 12.dp),
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (index < items.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}
