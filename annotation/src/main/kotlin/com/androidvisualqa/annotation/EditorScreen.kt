package com.androidvisualqa.annotation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * M1 annotation editor screen.
 *
 * Layout (top to bottom):
 * 1. Top app bar: Cancel chip (left) + title + Undo/Redo icons + Save chip (right).
 * 2. Frozen bitmap canvas with rectangle-drag overlay and pinch-to-zoom/one-finger-pan.
 * 3. Feedback text field pinned above the IME.
 * 4. Save bar pinned to bottom (// TODO(m2): review-and-save flow).
 *
 * Rectangle interaction: tap-drag-release draws one rectangle.
 * M1 caps at one rectangle; drawing a second replaces the first.
 *
 * Pinch-to-zoom: two-finger gesture, range 1.0×–4.0×.
 * One-finger drag: pans when zoomed in.
 *
 * @param state Current editor state.
 * @param onStateChange Callback for UI-local state changes (e.g. dragging flag).
 * @param onSave Called with the final rectangle and feedback text when Save is tapped.
 * @param onCancel Called when Cancel is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun EditorScreen(
    state: EditorState,
    onStateChange: (EditorState) -> Unit,
    onSave: (RectangleAnnotation?, String) -> Unit,
    onCancel: () -> Unit,
) {
    // Gesture tracking
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }
    var zoomScale by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var hasZoomOrPan by remember { mutableStateOf(false) }

    val currentRect = state.rectangles.lastOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Annotate",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    // Cancel chip
                    Surface(
                        onClick = onCancel,
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                },
                actions = {
                    // Undo
                    IconButton(
                        onClick = {
                            val s = state
                            if (s.undoStack.isNotEmpty()) {
                                val prev = s.undoStack.last()
                                onStateChange(
                                    prev.copy(
                                        undoStack = s.undoStack.dropLast(1),
                                        redoStack = s.redoStack + s,
                                    )
                                )
                            }
                        },
                        enabled = state.undoStack.isNotEmpty(),
                    ) {
                        // ponytail: simple text label instead of a vector icon for M1
                        Text(
                            "↩",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { contentDescription = "Undo" },
                        )
                    }
                    // Redo
                    IconButton(
                        onClick = {
                            val s = state
                            if (s.redoStack.isNotEmpty()) {
                                val next = s.redoStack.last()
                                onStateChange(
                                    next.copy(
                                        undoStack = s.undoStack + s,
                                        redoStack = s.redoStack.dropLast(1),
                                    )
                                )
                            }
                        },
                        enabled = state.redoStack.isNotEmpty(),
                    ) {
                        Text(
                            "↪",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { contentDescription = "Redo" },
                        )
                    }
                    // Save chip
                    Surface(
                        onClick = {
                            onSave(currentRect, state.feedbackText)
                        },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Canvas area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Annotation canvas" }
                        .aspectRatio(
                            if (state.bitmap != null && state.bitmap.width > 0 && state.bitmap.height > 0)
                                state.bitmap.width.toFloat() / state.bitmap.height.toFloat()
                            else 1f
                        )
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pointers = event.changes

                                    when {
                                        // Two-finger pinch-to-zoom
                                        pointers.size >= 2 -> {
                                            hasZoomOrPan = true
                                            val centroid = event.changes
                                                .map { it.position }
                                                .reduce { a, b -> a + b } / pointers.size.toFloat()
                                            val prevSpan = event.changes
                                                .map { it.previousPosition }
                                                .let { pts ->
                                                    if (pts.size >= 2) {
                                                        (pts[0] - pts[1]).getDistance()
                                                    } else 0f
                                                }
                                            val currSpan = event.changes
                                                .map { it.position }
                                                .let { pts ->
                                                    if (pts.size >= 2) {
                                                        (pts[0] - pts[1]).getDistance()
                                                    } else 0f
                                                }
                                            if (prevSpan > 0f && currSpan > 0f) {
                                                zoomScale = (zoomScale * (currSpan / prevSpan))
                                                    .coerceIn(1f, 4f)
                                            }
                                            event.changes.forEach { it.consume() }
                                        }
                                        // One-finger pan (only when zoomed in)
                                        pointers.size == 1 && hasZoomOrPan && zoomScale > 1f -> {
                                            val change = event.changes.first()
                                            panOffset += change.position - change.previousPosition
                                            change.consume()
                                        }
                                        // One-finger drag for rectangle drawing
                                        pointers.size == 1 && !hasZoomOrPan -> {
                                            val change = event.changes.first()
                                            if (change.pressed) {
                                                if (dragStart == null) {
                                                    dragStart = change.position
                                                }
                                                dragEnd = change.position
                                            } else {
                                                // Pointer up — commit rectangle
                                                val start = dragStart ?: change.position
                                                val end = change.position
                                                val canvasWidth = size.width.toFloat()
                                                val canvasHeight = size.height.toFloat()

                                                if (canvasWidth > 0f && canvasHeight > 0f &&
                                                    abs(end.x - start.x) > 8f &&
                                                    abs(end.y - start.y) > 8f
                                                ) {
                                                    val rect = RectangleAnnotation(
                                                        id = AnnotationId("rect-${System.currentTimeMillis()}"),
                                                        left = min(start.x, end.x) / canvasWidth,
                                                        top = min(start.y, end.y) / canvasHeight,
                                                        right = max(start.x, end.x) / canvasWidth,
                                                        bottom = max(start.y, end.y) / canvasHeight,
                                                        color = 0xFF6750A4L, // purple-40 primary
                                                    )
                                                    // Replace existing rectangle (M1 caps at 1)
                                                    val newRects = if (state.rectangles.size >= 1) {
                                                        listOf(rect)
                                                    } else {
                                                        state.rectangles + rect
                                                    }
                                                    onStateChange(
                                                        state.copy(
                                                            rectangles = newRects,
                                                            undoStack = state.undoStack + state,
                                                            redoStack = emptyList(),
                                                        )
                                                    )
                                                }
                                                dragStart = null
                                                dragEnd = null
                                            }
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        },
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // Apply zoom and pan
                    val scale = zoomScale
                    val panX = panOffset.x
                    val panY = panOffset.y

                    clipRect {
                        // Draw the frozen bitmap
                        val bmp = state.bitmap ?: return@clipRect
                        drawImage(
                            image = bmp,
                            dstSize = androidx.compose.ui.unit.IntSize(canvasWidth.toInt(), canvasHeight.toInt()),
                        )

                        // Draw existing rectangle
                        val rect = state.rectangles.lastOrNull()
                        if (rect != null) {
                            drawRect(
                                color = Color(rect.color),
                                topLeft = Offset(
                                    rect.left * canvasWidth + panX,
                                    rect.top * canvasHeight + panY,
                                ),
                                size = Size(
                                    (rect.right - rect.left) * canvasWidth,
                                    (rect.bottom - rect.top) * canvasHeight,
                                ),
                                style = Stroke(width = 3.dp.toPx()),
                            )
                        }

                        // Draw in-progress rectangle
                        val s = dragStart
                        val e = dragEnd
                        if (s != null && e != null) {
                            drawRect(
                                color = Color(0xFF6750A4L),
                                topLeft = Offset(
                                    min(s.x, e.x) + panX,
                                    min(s.y, e.y) + panY,
                                ),
                                size = Size(
                                    abs(e.x - s.x),
                                    abs(e.y - s.y),
                                ),
                                style = Stroke(width = 3.dp.toPx()),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Feedback text field
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                BasicTextField(
                    value = state.feedbackText,
                    onValueChange = { text ->
                        onStateChange(state.copy(feedbackText = text))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .padding(12.dp),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    ),
                    decorationBox = { innerTextField ->
                        if (state.feedbackText.isEmpty()) {
                            Text(
                                text = "Describe the issue…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save bar (pinned bottom)
            // TODO(m2): replace with full review-and-save flow
        }
    }
}
