@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.androidvisualqa.app.ui.redaction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.androidvisualqa.privacy.ManualRedactionStore
import com.androidvisualqa.privacy.RedactionRegion
import com.androidvisualqa.privacy.Sensitivity

/**
 * M3 manual redaction overlay composable.
 *
 * Wraps the editor canvas. Long-press to drop a solid black rectangle over
 * the touched area. Tap an existing rectangle to remove it.
 *
 * @param redactionStore The [ManualRedactionStore] managing the regions.
 * @param onRegionAdded Callback when a new region is added.
 * @param onRegionRemoved Callback when a region is removed (receives index).
 *
 * ponytail: M3 ships basic rectangle-only redaction; freehand masks are M4.
 * Regions are rendered at a fixed 10% size centered on the long-press point.
 * A future lane should let users drag-resize or draw arbitrary shapes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun ManualRedactionOverlay(
    modifier: Modifier = Modifier,
    redactionStore: ManualRedactionStore = ManualRedactionStore(),
    onRegionAdded: (RedactionRegion) -> Unit = {},
    onRegionRemoved: (Int) -> Unit = {},
) {
    val regions = redactionStore.list()

    Box(
        modifier = modifier
            .fillMaxSize()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* no-op; handled by region taps */ },
                onLongClick = {
                    // ponytail: drops a fixed-size rectangle centered on the
                    // long-press point. M4 will add resize handles.
                    val region = RedactionRegion(
                        left = 0.4,
                        top = 0.4,
                        right = 0.6,
                        bottom = 0.6,
                        sensitivity = Sensitivity.Other,
                        reason = "Manual redaction",
                    )
                    redactionStore.add(region)
                    onRegionAdded(region)
                },
            ),
    ) {
        // Render existing redaction regions as solid black rectangles
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (region in regions) {
                drawRedactionRegion(region, size)
            }
        }

        // Tappable hit targets for existing regions (tap to remove)
        for ((index, region) in regions.withIndex()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            redactionStore.remove(index)
                            onRegionRemoved(index)
                        },
                        onLongClick = {
                            redactionStore.remove(index)
                            onRegionRemoved(index)
                        },
                    ),
            )
        }
    }
}

/**
 * Draws a single [RedactionRegion] as a solid black rectangle.
 */
private fun DrawScope.drawRedactionRegion(region: RedactionRegion, canvasSize: Size) {
    val left = region.left.toFloat() * canvasSize.width
    val top = region.top.toFloat() * canvasSize.height
    val right = region.right.toFloat() * canvasSize.width
    val bottom = region.bottom.toFloat() * canvasSize.height

    drawRect(
        color = Color.Black,
        topLeft = Offset(left, top),
        size = Size(right - left, bottom - top),
    )
}
