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
 * @property rectangles Current list of rectangle annotations (M1 caps at 1).
 * @property undoStack Previous editor states for undo.
 * @property redoStack Future editor states for redo.
 * @property feedbackText User-typed feedback text below the canvas.
 * @property isDragging True while a rectangle drag is in progress.
 */
public data class EditorState(
    val bitmap: androidx.compose.ui.graphics.ImageBitmap? = null,
    val rectangles: List<RectangleAnnotation> = emptyList(),
    val undoStack: List<EditorState> = emptyList(),
    val redoStack: List<EditorState> = emptyList(),
    val feedbackText: String = "",
    val isDragging: Boolean = false,
) {
    /** Returns a non-null bitmap or throws. Convenience for Composable call-sites. */
    public fun requireBitmap(): androidx.compose.ui.graphics.ImageBitmap =
        checkNotNull(bitmap) { "EditorState.bitmap was not initialised" }
}
