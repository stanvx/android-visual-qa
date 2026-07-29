package com.androidvisualqa.annotation

/**
 * Complete UI state for the annotation editor screen.
 *
 * This is a single immutable snapshot. Undo/redo stacks store previous
 * [EditorState] values; [undoStack] and [redoStack] are the linear stacks
 * of prior and future states respectively.
 *
 * @property bitmap The frozen-frame bitmap being annotated. Null only during
 *                   initialisation or testing; the editor always provides one.
 * @property annotations Current selectable annotations. This is the source of
 *                   truth for the multi-tool editor.
 * @property rectangles Deprecated rectangle-only compatibility projection.
 * @property undoStack Previous editor states for undo.
 * @property redoStack Future editor states for redo.
 * @property feedbackText User-typed feedback text below the canvas.
 * @property isDragging True while a rectangle drag is in progress.
 */
public data class EditorState(
    val bitmap: androidx.compose.ui.graphics.ImageBitmap? = null,
    val rectangles: List<RectangleAnnotation> = emptyList(),
    val annotations: List<AnnotationItem> = emptyList(),
    val selectedAnnotationId: AnnotationId? = null,
    val activeTool: EditorTool = EditorTool.Comment,
    val snapCandidates: List<NormalizedBounds> = emptyList(),
    val undoStack: List<EditorState> = emptyList(),
    val redoStack: List<EditorState> = emptyList(),
    val feedbackText: String = "",
    val isDragging: Boolean = false,
) {
    /** The captured bitmap is never mutated by annotation operations. */
    public val originalBitmap: androidx.compose.ui.graphics.ImageBitmap?
        get() = bitmap

    /** Returns annotations, adapting old rectangle-only state when necessary. */
    public fun editableAnnotations(): List<AnnotationItem> =
        if (annotations.isNotEmpty()) annotations else rectangles.map { it.asItem() }

    public fun selectedAnnotation(): AnnotationItem? =
        editableAnnotations().firstOrNull { it.id == selectedAnnotationId }

    /** Records one content mutation and clears redo history. */
    public fun commit(next: EditorState): EditorState =
        next.copy(
            undoStack = undoStack + withoutHistory(),
            redoStack = emptyList(),
        )

    public fun undo(): EditorState {
        if (undoStack.isEmpty()) return this
        return undoStack.last().copy(
            undoStack = undoStack.dropLast(1),
            redoStack = redoStack + withoutHistory(),
        )
    }

    public fun redo(): EditorState {
        if (redoStack.isEmpty()) return this
        return redoStack.last().copy(
            undoStack = undoStack + withoutHistory(),
            redoStack = redoStack.dropLast(1),
        )
    }

    public fun withAnnotations(items: List<AnnotationItem>, selected: AnnotationId? = selectedAnnotationId): EditorState =
        copy(
            annotations = items,
            rectangles = items.mapNotNull { it.asRectangleOrNull() },
            selectedAnnotationId = selected,
        )

    public fun withSnapCandidates(candidates: List<NormalizedBounds>): EditorState =
        copy(snapCandidates = candidates)

    /** Returns a non-null bitmap or throws. Convenience for Composable call-sites. */
    public fun requireBitmap(): androidx.compose.ui.graphics.ImageBitmap =
        checkNotNull(bitmap) { "EditorState.bitmap was not initialised" }

    private fun withoutHistory(): EditorState = copy(undoStack = emptyList(), redoStack = emptyList())
}

public enum class EditorTool {
    Select,
    Comment,
    Rectangle,
    Pen,
    Arrow,
    TextNote,
}

private fun RectangleAnnotation.asItem(): AnnotationItem = toAnnotationItem()

private fun AnnotationItem.asRectangleOrNull(): RectangleAnnotation? =
    (geometry as? AnnotationGeometry.Region)?.let { region ->
        RectangleAnnotation(
            id = id,
            left = region.bounds.left,
            top = region.bounds.top,
            right = region.bounds.right,
            bottom = region.bounds.bottom,
            color = color,
        )
    }
