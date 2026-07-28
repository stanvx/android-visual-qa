package com.androidvisualqa.app

import com.androidvisualqa.annotation.AnnotationId
import com.androidvisualqa.annotation.EditorState
import com.androidvisualqa.annotation.RectangleAnnotation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * JVM-level verification of the diff-based state routing that
 * [EditorScreenWrapper] applies when forwarding [EditorState] changes
 * to the ViewModel.
 *
 * Since the full Composable integration requires an Android/Compose runtime,
 * this test verifies the diff logic in isolation: given a previous and a new
 * [EditorState], the correct action is identified.
 *
 * // TODO(m2): add Robolectric tests for the full ViewModel↔Screen wiring.
 */
class EditorScreenWrapperTest {

    @Test
    fun `new rectangle is detected`() {
        val prev = EditorState()
        val rect = RectangleAnnotation(
            id = AnnotationId("r1"),
            left = 0.1f, top = 0.1f, right = 0.5f, bottom = 0.5f,
            color = 0xFF6750A4L,
        )
        val next = prev.copy(
            rectangles = prev.rectangles + rect,
            undoStack = prev.undoStack + prev,
            redoStack = emptyList(),
        )
        // Simulate the diff logic from EditorScreenWrapper
        val action = detectAction(prev, next)
        assertEquals("addRectangle", action)
    }

    @Test
    fun `undo is detected when undo stack shrinks`() {
        val rect = RectangleAnnotation(
            id = AnnotationId("r1"),
            left = 0.1f, top = 0.1f, right = 0.5f, bottom = 0.5f,
            color = 0xFF6750A4L,
        )
        val empty = EditorState()
        val afterAdd = empty.copy(
            rectangles = empty.rectangles + rect,
            undoStack = empty.undoStack + empty,
        )
        // Undo restores empty
        val afterUndo = afterAdd.undoStack.last().copy(
            undoStack = afterAdd.undoStack.dropLast(1),
            redoStack = afterAdd.redoStack + afterAdd,
        )
        val action = detectAction(afterAdd, afterUndo)
        assertEquals("undo", action)
    }

    @Test
    fun `redo is detected when redo stack shrinks`() {
        val rect = RectangleAnnotation(
            id = AnnotationId("r1"),
            left = 0.1f, top = 0.1f, right = 0.5f, bottom = 0.5f,
            color = 0xFF6750A4L,
        )
        val empty = EditorState()
        val afterAdd = empty.copy(
            rectangles = empty.rectangles + rect,
            undoStack = empty.undoStack + empty,
        )
        val afterUndo = afterAdd.undoStack.last().copy(
            undoStack = afterAdd.undoStack.dropLast(1),
            redoStack = afterAdd.redoStack + afterAdd,
        )
        // Redo restores afterAdd
        val afterRedo = afterUndo.redoStack.last().copy(
            undoStack = afterUndo.undoStack + afterUndo,
            redoStack = afterUndo.redoStack.dropLast(1),
        )
        val action = detectAction(afterUndo, afterRedo)
        assertEquals("redo", action)
    }

    @Test
    fun `feedback text change is detected`() {
        val prev = EditorState()
        val next = prev.copy(feedbackText = "New text")
        val action = detectAction(prev, next)
        assertEquals("setFeedbackText", action)
    }

    @Test
    fun `no action when nothing meaningful changes`() {
        val prev = EditorState()
        val next = prev.copy(isDragging = true) // isDragging is UI-local, not ViewModel-driven
        val action = detectAction(prev, next)
        assertEquals("none", action)
    }

    // --- The exact diff logic from EditorScreenWrapper (inlined for testability) ---

    private fun detectAction(prev: EditorState, next: EditorState): String = when {
        prev.undoStack.size > next.undoStack.size -> "undo"
        prev.redoStack.size > next.redoStack.size -> "redo"
        next.rectangles.size > prev.rectangles.size -> "addRectangle"
        next.feedbackText != prev.feedbackText -> "setFeedbackText"
        else -> "none"
    }
}
