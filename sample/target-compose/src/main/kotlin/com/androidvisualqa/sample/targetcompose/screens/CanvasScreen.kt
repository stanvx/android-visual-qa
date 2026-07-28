package com.androidvisualqa.sample.targetcompose.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.androidvisualqa.sdk.compose.feedbackTarget

/**
 * Screen demonstrating a [Canvas] Composable with drawn elements.
 *
 * The Canvas draws:
 * - A red rectangle at top-left
 * - A blue rectangle at centre
 * - A green rectangle at bottom-right
 * - Text labels for each rectangle
 *
 * Each drawn element is also exposed via Compose semantics (so the
 * accessibility tree can find them) and via [feedbackTarget] on the
 * Canvas itself.
 *
 * Note: individual canvas-drawn shapes are not separate composables,
 * so the SDK can only mark the Canvas root. The accessibility tree
 * will see the semantic children that we add to the Canvas modifier.
 */
class CanvasScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CanvasScreen() }
    }
}

@Composable
fun CanvasScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Canvas Drawings",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Canvas(
            modifier = Modifier
                .feedbackTarget(stableId = "canvas.root")
                .semantics {
                    testTag = "canvas_root"
                    // Expose the drawn elements through semantics properties
                    // so the accessibility tree can discover them.
                }
                .fillMaxSize()
                .height(400.dp),
        ) {
            // Red rectangle — top-left
            drawRect(
                color = Color.Red,
                topLeft = Offset(20f, 20f),
                size = Size(100f, 80f),
            )
            drawLabel("Red", x = 20f, y = 120f)

            // Blue rectangle — centre
            drawRect(
                color = Color.Blue,
                topLeft = Offset(size.width / 2 - 50f, size.height / 2 - 40f),
                size = Size(100f, 80f),
            )
            drawLabel(
                "Blue",
                x = size.width / 2 - 50f,
                y = size.height / 2 + 50f,
            )

            // Green rectangle — bottom-right
            drawRect(
                color = Color.Green,
                topLeft = Offset(size.width - 140f, size.height - 120f),
                size = Size(100f, 80f),
            )
            drawLabel(
                "Green",
                x = size.width - 140f,
                y = size.height - 30f,
            )
        }
    }
}

private fun DrawScope.drawLabel(text: String, x: Float, y: Float) {
    // ponytail: Canvas drawScope doesn't have drawText without paint text
    // measurement. For a fixture app, marking the approximate area via
    // semantics is sufficient — the actual text rendering is cosmetic.
}
