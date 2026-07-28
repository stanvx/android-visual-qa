package com.androidvisualqa.sample.targetcompose.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.androidvisualqa.sdk.compose.feedbackTarget

/**
 * Screen whose layout differs in portrait vs landscape.
 *
 * Uses [LocalConfiguration.current.orientation] to switch between
 * a vertical [Column] (portrait) and a horizontal [Row] (landscape).
 *
 * Used to verify that the SDK correctly re-registers components
 * after configuration change (bounds change, recomposition).
 */
class RotationScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RotationScreen() }
    }
}

@Composable
fun RotationScreen() {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    Column(
        modifier = Modifier
            .feedbackTarget(stableId = "rotation.screen.root")
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Rotation Demo",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(
            text = "Current: ${if (isLandscape) "Landscape" else "Portrait"}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .feedbackTarget(stableId = "rotation.content")
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Left panel", modifier = Modifier.feedbackTarget(stableId = "rotation.panel.left"))
                Text("Right panel", modifier = Modifier.feedbackTarget(stableId = "rotation.panel.right"))
            }
        } else {
            Column(
                modifier = Modifier
                    .feedbackTarget(stableId = "rotation.content")
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Top item", modifier = Modifier.feedbackTarget(stableId = "rotation.item.top"))
                Text("Bottom item", modifier = Modifier.feedbackTarget(stableId = "rotation.item.bottom"))
            }
        }
    }
}
