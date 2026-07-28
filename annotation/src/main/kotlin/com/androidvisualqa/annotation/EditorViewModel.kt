package com.androidvisualqa.annotation

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.files.FileSystemDraftStore
import com.androidvisualqa.files.DraftDirectory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Path

/**
 * ViewModel bridging the [EditorScreen] Composable to the draft storage layer.
 *
 * On init, attempts to load a placeholder bitmap:
 * - If [draftId] is provided and a draft exists at the store path, reads the
 *   original screenshot bytes and decodes them to an [ImageBitmap].
 * - Otherwise generates a solid-colour placeholder so the editor runs in isolation.
 *
 * // TODO(m2): replace placeholder with real PixelCaptureSource.trigger()
 */
public class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val store: FileSystemDraftStore by lazy {
        val dir = application.getDir("drafts", android.content.Context.MODE_PRIVATE)
        FileSystemDraftStore(DraftDirectory(dir.toPath()))
    }

    private val _state: MutableStateFlow<EditorState> = MutableStateFlow(createPlaceholderState())
    public val state: StateFlow<EditorState> = _state.asStateFlow()

    /** The draft ID loaded, or null for a new unsaved draft. */
    private var currentDraftId: DraftId? = null

    /**
     * Loads an existing draft by [draftId] or starts a new blank session.
     */
    public fun loadDraft(draftId: DraftId? = null) {
        currentDraftId = draftId
        viewModelScope.launch {
            val bitmap = if (draftId != null) {
                loadBitmapFromDraft(draftId) ?: generatePlaceholderBitmap()
            } else {
                generatePlaceholderBitmap()
            }
            _state.value = EditorState(bitmap = bitmap)
        }
    }

    public fun addRectangle(rect: RectangleAnnotation) {
        val current = _state.value
        _state.value = current.copy(
            rectangles = current.rectangles + rect,
            undoStack = current.undoStack + listOf(current),
            redoStack = emptyList(),
            isDragging = false,
        )
    }

    public fun undo() {
        val current = _state.value
        if (current.undoStack.isEmpty()) return
        val previous = current.undoStack.last()
        _state.value = previous.copy(
            undoStack = current.undoStack.dropLast(1),
            redoStack = current.redoStack + listOf(current),
        )
    }

    public fun redo() {
        val current = _state.value
        if (current.redoStack.isEmpty()) return
        val next = current.redoStack.last()
        _state.value = next.copy(
            undoStack = current.undoStack + listOf(current),
            redoStack = current.redoStack.dropLast(1),
        )
    }

    public fun setFeedbackText(text: String) {
        _state.value = _state.value.copy(feedbackText = text)
    }

    public fun setDragging(dragging: Boolean) {
        _state.value = _state.value.copy(isDragging = dragging)
    }

    /**
     * Saves the current annotation as a new draft and invokes [onSave]
     * with the rectangle and feedback text.
     *
     * // TODO(m2): wire ReportAssembler so save() produces report.json etc.
     */
    public fun save(onSave: (RectangleAnnotation?, String) -> Unit) {
        val s = _state.value
        val rect = s.rectangles.lastOrNull()
        onSave(rect, s.feedbackText)

        viewModelScope.launch {
            val id = currentDraftId ?: store.createDraft().getOrNull()
            if (id != null) {
                currentDraftId = id
                // TODO(m2): write annotated bitmap via store.writeAnnotated()
            }
        }
    }

    // --- Private helpers ---

    private fun createPlaceholderState(): EditorState =
        EditorState(bitmap = generatePlaceholderBitmap())

    private fun loadBitmapFromDraft(draftId: DraftId): ImageBitmap? {
        // TODO(m2): real FileSystemDraftStore.readOriginalImage() call
        return null
    }

    private fun generatePlaceholderBitmap(): ImageBitmap {
        // 480×800 placeholder — solid light-grey surface
        val cfg = android.graphics.Bitmap.Config.ARGB_8888
        val bmp = android.graphics.Bitmap.createBitmap(480, 800, cfg)
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawColor(android.graphics.Color.argb(255, 230, 230, 230))
        return bmp.asImageBitmap()
    }
}
