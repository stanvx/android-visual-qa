package com.androidvisualqa.annotation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * JVM-level state-model tests for [EditorState], [RectangleAnnotation],
 * and the undo/redo contract.
 *
 * [EditorState.bitmap] is nullable; these tests use `null` for the bitmap
 * value since Compose's [androidx.compose.ui.graphics.ImageBitmap] cannot
 * be instantiated in a pure JVM unit-test environment.
 *
 * // ponytail: No Compose UI rendering is tested here — only pure data-model
 * // mutations (rectangle add, undo, redo, text change). Compose UI tests
 * // require an emulator and ship in M2.
 */
class EditorStateTest {

    @Test
    fun `empty state has no rectangles`() {
        val state = EditorState()
        assertTrue(state.rectangles.isEmpty())
        assertEquals("", state.feedbackText)
    }

    @Test
    fun `add rectangle updates rectangles list`() {
        val state = EditorState()
        val rect = rect("r1")
        val updated = state.copy(
            rectangles = state.rectangles + rect,
            undoStack = state.undoStack + state,
        )
        assertEquals(1, updated.rectangles.size)
        assertEquals("r1", updated.rectangles.first().id.value)
        assertEquals(1, updated.undoStack.size)
    }

    @Test
    fun `undo restores previous rectangles list`() {
        val empty = EditorState()
        val rect = rect("r1")
        val afterAdd = empty.copy(
            rectangles = empty.rectangles + rect,
            undoStack = empty.undoStack + empty,
        )
        val afterUndo = afterAdd.undoStack.lastOrNull()?.copy(
            undoStack = afterAdd.undoStack.dropLast(1),
            redoStack = afterAdd.redoStack + afterAdd,
        )
        assertNotNull(afterUndo)
        assertTrue(afterUndo!!.rectangles.isEmpty())
    }

    @Test
    fun `redo after undo restores rectangle`() {
        val empty = EditorState()
        val rect = rect("r1")
        val afterAdd = empty.copy(
            rectangles = empty.rectangles + rect,
            undoStack = empty.undoStack + empty,
        )
        // Undo
        val afterUndo = afterAdd.undoStack.last().copy(
            undoStack = afterAdd.undoStack.dropLast(1),
            redoStack = afterAdd.redoStack + afterAdd,
        )
        // Redo
        val afterRedo = afterUndo.redoStack.last().copy(
            undoStack = afterUndo.undoStack + afterUndo,
            redoStack = afterUndo.redoStack.dropLast(1),
        )
        assertEquals(1, afterRedo.rectangles.size)
    }

    @Test
    fun `feedback text updates`() {
        val state = EditorState()
        val updated = state.copy(feedbackText = "The button is misaligned")
        assertEquals("The button is misaligned", updated.feedbackText)
    }

    @Test
    fun `isDragging flag toggles`() {
        val state = EditorState(isDragging = true)
        assertTrue(state.isDragging)
    }

    @Test
    fun `bitmap is null by default`() {
        val state = EditorState()
        assertTrue(state.bitmap == null)
    }

    // --- Test helpers ---

    private fun rect(id: String) = RectangleAnnotation(
        id = AnnotationId(id),
        left = 0.1f, top = 0.1f, right = 0.5f, bottom = 0.5f,
        color = 0xFF6750A4L,
    )
}
