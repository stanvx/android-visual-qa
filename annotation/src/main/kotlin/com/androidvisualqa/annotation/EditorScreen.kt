package com.androidvisualqa.annotation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Full-screen lasso editor. Draw around an item, release, then add a comment. */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
public fun EditorScreen(
    state: EditorState,
    onStateChange: (EditorState) -> Unit,
    onSave: (RectangleAnnotation?, String) -> Unit,
    onCancel: () -> Unit,
    onSaveAnnotations: ((List<AnnotationItem>, String) -> Unit)? = null,
) {
    var dragPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var commentingId by remember { mutableStateOf<AnnotationId?>(null) }
    var commentText by remember { mutableStateOf("") }
    val annotations = state.editableAnnotations()
    val selected = annotations.firstOrNull { it.id == commentingId }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    fun commit(items: List<AnnotationItem>, selectedId: AnnotationId? = state.selectedAnnotationId) {
        onStateChange(state.commit(state.withAnnotations(items, selectedId)))
    }

    fun finishComment() {
        val id = commentingId ?: return
        val updated = if (commentText.isBlank()) {
            annotations
        } else {
            annotations.map { item ->
                if (item.id == id) item.withComment(commentText.trim()) else item
            }
        }
        if (updated != annotations) commit(updated, id)
        commentingId = null
        commentText = ""
        keyboardController?.hide()
    }

    fun normalized(point: Offset, viewport: ImageViewport): NormalizedPoint =
        NormalizedPoint.from(
            (point.x - viewport.left) / viewport.width,
            (point.y - viewport.top) / viewport.height,
        )

    fun selectAt(point: Offset, viewport: ImageViewport) {
        if (!viewport.contains(point)) return
        val hit = annotations.lastOrNull { it.bounds.contains(normalized(point, viewport)) }
        if (hit == null) {
            onStateChange(state.copy(selectedAnnotationId = null))
            return
        }
        onStateChange(state.copy(selectedAnnotationId = hit.id))
        commentingId = hit.id
        commentText = ""
    }

    fun finishLasso(points: List<Offset>, size: IntSize) {
        val bitmap = state.bitmap ?: return
        val viewport = imageViewport(size, bitmap)
        if (points.size < 4 || !viewport.contains(points.first())) return

        val normalizedPoints = points.map { normalized(it, viewport) }
        val rawBounds = boundsOf(normalizedPoints)
        if (rawBounds.right - rawBounds.left < 0.015f || rawBounds.bottom - rawBounds.top < 0.015f) return

        val snappedBounds = snapRegion(rawBounds, state.snapCandidates)
        val snappedPoints = remapPoints(normalizedPoints, rawBounds, snappedBounds)
        val closedPoints = snappedPoints + snappedPoints.first()
        val item = AnnotationItem(
            id = AnnotationId("lasso-${System.currentTimeMillis()}"),
            geometry = AnnotationGeometry.Lasso(closedPoints, snappedBounds),
        )
        commit(annotations + item, item.id)
        commentingId = item.id
        commentText = ""
    }

    LaunchedEffect(commentingId) {
        if (commentingId != null) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .semantics { contentDescription = "Capture canvas. Draw a circle around an item to add a comment." }
                .pointerInput(state.bitmap, state.snapCandidates, commentingId) {
                    detectTapGestures { point ->
                        if (commentingId != null) finishComment()
                        else state.bitmap?.let { selectAt(point, imageViewport(canvasSize, it)) }
                    }
                }
                .pointerInput(state.bitmap, state.snapCandidates, commentingId) {
                    detectDragGestures(
                        onDragStart = { point ->
                            if (commentingId == null) dragPoints = listOf(point)
                        },
                        onDrag = { change, _ ->
                            if (commentingId == null && dragPoints.isNotEmpty()) {
                                dragPoints = dragPoints + change.position
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            if (commentingId == null) finishLasso(dragPoints, canvasSize)
                            dragPoints = emptyList()
                        },
                        onDragCancel = { dragPoints = emptyList() },
                    )
                },
        ) {
            val bitmap = state.bitmap
            val viewport = bitmap?.let { imageViewport(IntSize(size.width.roundToInt(), size.height.roundToInt()), it) }
            drawRect(Color.Black)
            if (bitmap != null && viewport != null) {
                drawImage(
                    bitmap,
                    dstOffset = IntOffset(viewport.left.roundToInt(), viewport.top.roundToInt()),
                    dstSize = IntSize(viewport.width.roundToInt(), viewport.height.roundToInt()),
                )
                annotations.forEach { item ->
                    drawAnnotation(
                        item = item,
                        viewport = viewport,
                        color = if (item.id == state.selectedAnnotationId) primary else Color(0xFF60D8E8),
                        selected = item.id == state.selectedAnnotationId,
                    )
                }
                if (dragPoints.size > 1) {
                    drawPath(
                        pathFromOffsets(dragPoints),
                        color = primary,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    drawCircle(primary, 5.dp.toPx(), dragPoints.first())
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp),
            color = Color.Black.copy(alpha = 0.58f),
            contentColor = Color.White,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.semantics { contentDescription = "Close capture" },
            ) { Text("×  Close", fontSize = 14.sp) }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp),
            color = Color.Black.copy(alpha = 0.58f),
            contentColor = Color.White,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Text(
                text = if (annotations.isEmpty()) "Draw around anything" else "${annotations.size} mark${if (annotations.size == 1) "" else "s"}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp),
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.58f),
                contentColor = Color.White,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Row {
                    IconButton(
                        onClick = { onStateChange(state.undo()) },
                        enabled = state.undoStack.isNotEmpty(),
                        modifier = Modifier.semantics { contentDescription = "Undo" },
                    ) { Text("↶", fontSize = 22.sp) }
                    IconButton(
                        onClick = { onStateChange(state.redo()) },
                        enabled = state.redoStack.isNotEmpty(),
                        modifier = Modifier.semantics { contentDescription = "Redo" },
                    ) { Text("↷", fontSize = 22.sp) }
                }
            }
        }

        if (annotations.isEmpty() && dragPoints.isEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Black.copy(alpha = 0.68f),
                contentColor = Color.White,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Circle an item", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Release to add a comment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                    )
                }
            }
        }

        if (selected != null && commentingId != null && canvasSize != IntSize.Zero && state.bitmap != null) {
            val viewport = imageViewport(canvasSize, state.bitmap)
            val anchor = commentAnchor(
                selected.bounds,
                viewport,
                canvasSize,
                with(density) { 340.dp.toPx() },
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(anchor.x.roundToInt(), anchor.y.roundToInt())
                    }
                    .widthIn(max = 340.dp)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (selected.comments.isEmpty()) "Add a comment" else "Add another comment",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = ::finishComment) { Text("Done") }
                    }
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text("What should change here?") },
                        minLines = 2,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { finishComment() }),
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (commentingId != null) finishComment()
                val items = state.editableAnnotations()
                if (onSaveAnnotations != null) onSaveAnnotations(items, state.feedbackText)
                else onSave(items.lastOrNull()?.toRectangleOrNull(), state.feedbackText)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp)
                .semantics { contentDescription = "Save capture" },
            containerColor = primary,
            contentColor = onPrimary,
        ) { Text("✓", fontSize = 22.sp) }
    }
}

private data class ImageViewport(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    fun contains(point: Offset): Boolean =
        point.x in left..(left + width) && point.y in top..(top + height)
}

private fun imageViewport(size: IntSize, bitmap: ImageBitmap): ImageViewport {
    val bitmapWidth = bitmap.width.toFloat().coerceAtLeast(1f)
    val bitmapHeight = bitmap.height.toFloat().coerceAtLeast(1f)
    val scale = min(size.width / bitmapWidth, size.height / bitmapHeight)
    val width = bitmapWidth * scale
    val height = bitmapHeight * scale
    return ImageViewport(
        left = (size.width - width) / 2f,
        top = (size.height - height) / 2f,
        width = width,
        height = height,
    )
}

private fun boundsOf(points: List<NormalizedPoint>): NormalizedBounds {
    if (points.isEmpty()) return NormalizedBounds.Empty
    return NormalizedBounds.from(
        points.minOf { it.x },
        points.minOf { it.y },
        points.maxOf { it.x },
        points.maxOf { it.y },
    )
}

private fun remapPoints(
    points: List<NormalizedPoint>,
    from: NormalizedBounds,
    to: NormalizedBounds,
): List<NormalizedPoint> {
    val fromWidth = (from.right - from.left).coerceAtLeast(0.001f)
    val fromHeight = (from.bottom - from.top).coerceAtLeast(0.001f)
    return points.map { point ->
        NormalizedPoint.from(
            to.left + ((point.x - from.left) / fromWidth) * (to.right - to.left),
            to.top + ((point.y - from.top) / fromHeight) * (to.bottom - to.top),
        )
    }
}

private fun pathFromOffsets(points: List<Offset>): Path = Path().apply {
    points.firstOrNull()?.let { moveTo(it.x, it.y) }
    points.drop(1).forEach { lineTo(it.x, it.y) }
}

private fun commentAnchor(
    bounds: NormalizedBounds,
    viewport: ImageViewport,
    canvasSize: IntSize,
    bubbleWidth: Float,
): Offset {
    val x = (viewport.left + bounds.right * viewport.width + 12f)
        .coerceIn(12f, (canvasSize.width - bubbleWidth - 12f).coerceAtLeast(12f))
    val y = (viewport.top + bounds.top * viewport.height - 168f)
        .coerceIn(78f, (canvasSize.height - 250f).coerceAtLeast(78f))
    return Offset(x, y)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAnnotation(
    item: AnnotationItem,
    viewport: ImageViewport,
    color: Color,
    selected: Boolean,
) {
    val stroke = Stroke(
        width = if (selected) 5.dp.toPx() else 3.dp.toPx(),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
    when (val geometry = item.geometry) {
        is AnnotationGeometry.Lasso -> {
            val path = pathFromNormalized(geometry.points, viewport).apply { close() }
            drawPath(path, color.copy(alpha = if (selected) 0.14f else 0.08f))
            drawPath(path, color, style = stroke)
        }
        is AnnotationGeometry.Region -> {
            val bounds = geometry.bounds.toRect(viewport)
            drawRoundRect(
                color = color.copy(alpha = 0.1f),
                topLeft = bounds.topLeft,
                size = bounds.size,
                cornerRadius = CornerRadius(min(bounds.right - bounds.left, bounds.bottom - bounds.top) / 2f),
            )
            drawRoundRect(
                color,
                bounds.topLeft,
                bounds.size,
                CornerRadius(min(bounds.right - bounds.left, bounds.bottom - bounds.top) / 2f),
                style = stroke,
            )
        }
        is AnnotationGeometry.Freehand -> drawPath(pathFromNormalized(geometry.points, viewport), color, style = stroke)
        is AnnotationGeometry.Arrow -> drawLine(
            color,
            Offset(viewport.left + geometry.start.x * viewport.width, viewport.top + geometry.start.y * viewport.height),
            Offset(viewport.left + geometry.end.x * viewport.width, viewport.top + geometry.end.y * viewport.height),
            stroke.width,
            cap = StrokeCap.Round,
        )
        is AnnotationGeometry.TextNote -> drawCircle(
            color,
            12.dp.toPx(),
            Offset(viewport.left + geometry.position.x * viewport.width, viewport.top + geometry.position.y * viewport.height),
        )
        is AnnotationGeometry.CommentMarker -> drawCircle(
            color,
            14.dp.toPx(),
            Offset(viewport.left + geometry.position.x * viewport.width, viewport.top + geometry.position.y * viewport.height),
            style = stroke,
        )
    }
    if (item.comments.isNotEmpty()) {
        val bounds = item.bounds.toRect(viewport)
        drawCircle(color, 8.dp.toPx(), Offset(bounds.right, bounds.top))
    }
}

private fun pathFromNormalized(points: List<NormalizedPoint>, viewport: ImageViewport): Path = Path().apply {
    points.firstOrNull()?.let {
        moveTo(viewport.left + it.x * viewport.width, viewport.top + it.y * viewport.height)
    }
    points.drop(1).forEach {
        lineTo(viewport.left + it.x * viewport.width, viewport.top + it.y * viewport.height)
    }
}

private fun NormalizedBounds.toRect(viewport: ImageViewport): Rect = Rect(
    left = viewport.left + left * viewport.width,
    top = viewport.top + top * viewport.height,
    right = viewport.left + right * viewport.width,
    bottom = viewport.top + bottom * viewport.height,
)
