package com.androidvisualqa.annotation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Review editor for one captured screen.
 *
 * Layout (top to bottom):
 * 1. Top app bar: exit, title, and undo/redo actions.
 * 2. Tonal image surface with rectangle-drag overlay and pinch-to-zoom/one-finger-pan.
 * 3. Clear selection guidance and an accessible feedback field.
 * 4. Thumb-reachable save action pinned to the bottom.
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
                    Column {
                        Text(text = "Review capture", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = if (currentRect == null) "Drag to mark the issue" else "Area marked",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onCancel) {
                        Text(text = "Cancel")
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Button(
                    onClick = { onSave(currentRect, state.feedbackText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text(text = "Save report")
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 2.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(MaterialTheme.shapes.medium)
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
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (currentRect == null) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                ) {
                    Text(
                        text = if (currentRect == null) "Drag across the problem area" else "Marked area will be included",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (currentRect == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                if (currentRect != null) {
                    Text(
                        text = "Draw again to replace",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = state.feedbackText,
                onValueChange = { text ->
                    onStateChange(state.copy(feedbackText = text))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(112.dp),
                label = { Text(text = "Feedback (optional)") },
                placeholder = { Text(text = "What should change on this screen?") },
                minLines = 3,
                maxLines = 4,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
