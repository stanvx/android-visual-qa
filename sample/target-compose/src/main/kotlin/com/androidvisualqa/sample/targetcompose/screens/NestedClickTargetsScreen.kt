package com.androidvisualqa.sample.targetcompose.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.androidvisualqa.sdk.compose.feedbackTarget

/**
 * Screen with three levels of nested clickable regions.
 *
 * Layout hierarchy:
 *   Card (outer)                      → feedbackTarget("nested.outer")
 *     └── Button (inner)              → feedbackTarget("nested.inner")
 *           └── ImageButton (innermost) → feedbackTarget("nested.innermost")
 *
 * Each clickable has a distinct stable ID. The bounding boxes are nested
 * (the innermost is fully contained by the inner, which is fully contained
 * by the outer). This exercises the matching engine's bounding-box nesting
 * resolution: the engine must rank the innermost target highest when the
 * user taps the centre of the image, and must demote the outer targets
 * according to their overlap with the tap point.
 */
class NestedClickTargetsScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NestedClickTargetsScreen() }
    }
}

@Composable
fun NestedClickTargetsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Nested Click Targets",
            style = MaterialTheme.typography.headlineSmall,
        )

        // Outer card
        Card(
            modifier = Modifier
                .feedbackTarget(stableId = "nested.outer")
                .clickable { /* outer tap */ }
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Box(
                modifier = Modifier.padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Outer Card",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    // Inner button
                    Button(
                        onClick = { /* inner click */ },
                        modifier = Modifier
                            .feedbackTarget(stableId = "nested.inner")
                            .padding(8.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Inner Button")

                            // Innermost image button
                            Image(
                                painter = painterResource(android.R.drawable.ic_menu_camera),
                                contentDescription = "Camera icon",
                                modifier = Modifier
                                    .feedbackTarget(stableId = "nested.innermost")
                                    .clickable { /* innermost click */ }
                                    .size(32.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
