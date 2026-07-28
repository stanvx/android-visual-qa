package com.androidvisualqa.sample.targetcompose.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidvisualqa.sdk.compose.feedbackTarget

/**
 * Screen using [WindowSizeClass] (approximated via screen width dp)
 * to switch between phone, foldable, and tablet layouts.
 *
 * Layout modes:
 * - **Compact** (< 600dp): Single-column list.
 * - **Medium** (600dp–840dp): Two-column list with master-detail hint.
 * - **Expanded** (> 840dp): Three-column layout with sidebar + content + detail.
 *
 * Used to verify the SDK across form factors without a device farm.
 * The activity content changes based on screen width class.
 */
class TabletLayoutScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TabletLayoutScreen() }
    }
}

@Composable
fun TabletLayoutScreen() {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val windowSizeClass = when {
        screenWidthDp < 600 -> WindowSizeClass.Compact
        screenWidthDp < 840 -> WindowSizeClass.Medium
        else -> WindowSizeClass.Expanded
    }

    Column(
        modifier = Modifier
            .feedbackTarget(stableId = "tablet.screen.root")
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Tablet Layout Demo",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = "Window width: ${screenWidthDp}dp  →  ${windowSizeClass.name}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        when (windowSizeClass) {
            WindowSizeClass.Compact -> CompactLayout()
            WindowSizeClass.Medium -> MediumLayout()
            WindowSizeClass.Expanded -> ExpandedLayout()
        }
    }
}

private enum class WindowSizeClass {
    Compact, Medium, Expanded,
}

@Composable
private fun CompactLayout() {
    Column(
        modifier = Modifier
            .feedbackTarget(stableId = "tablet.layout.compact")
            .fillMaxSize(),
    ) {
        Text(
            text = "Single-column (compact)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        repeat(5) { i ->
            Box(
                modifier = Modifier
                    .feedbackTarget(stableId = "tablet.compact.item.$i")
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFFE0E0E0))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text("List item $i")
            }
            if (i < 4) HorizontalDivider()
        }
    }
}

@Composable
private fun MediumLayout() {
    Column(
        modifier = Modifier
            .feedbackTarget(stableId = "tablet.layout.medium")
            .fillMaxSize(),
    ) {
        Text(
            text = "Two-column (medium) — master/detail",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .feedbackTarget(stableId = "tablet.medium.master"),
            ) {
                Text("Master list", style = MaterialTheme.typography.titleSmall)
            }
            Column(
                modifier = Modifier
                    .weight(2f)
                    .feedbackTarget(stableId = "tablet.medium.detail"),
            ) {
                Text("Detail pane", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun ExpandedLayout() {
    Column(
        modifier = Modifier
            .feedbackTarget(stableId = "tablet.layout.expanded")
            .fillMaxSize(),
    ) {
        Text(
            text = "Three-column (expanded) — sidebar / content / detail",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .feedbackTarget(stableId = "tablet.expanded.sidebar")
                    .padding(end = 4.dp),
            ) {
                Text("Sidebar", style = MaterialTheme.typography.titleSmall)
            }
            Column(
                modifier = Modifier
                    .weight(2f)
                    .feedbackTarget(stableId = "tablet.expanded.content")
                    .padding(horizontal = 4.dp),
            ) {
                Text("Content", style = MaterialTheme.typography.titleSmall)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .feedbackTarget(stableId = "tablet.expanded.detail")
                    .padding(start = 4.dp),
            ) {
                Text("Detail", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
