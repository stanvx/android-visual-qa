package com.androidvisualqa.annotation

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * ViewModel bridging the [EditorScreen] Composable to the draft storage layer.
 *
 * On init, loads the captured bitmap:
 * - If [draftId] is provided and a draft exists at the store path, reads the
 *   original screenshot bytes and decodes them to an [ImageBitmap].
 * - Otherwise generates a solid-colour placeholder so the editor runs in isolation.
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
        // Guard: prevent undo-stack accumulation of null-bitmap states
        if (_state.value.bitmap == null) return
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
     * Persists the annotated bitmap, then invokes [onSave] with the rectangle
     * and feedback text so the app can finish the report.
     */
    public fun save(onSave: (RectangleAnnotation?, String) -> Unit) {
        val s = _state.value
        val rect = s.rectangles.lastOrNull()

        viewModelScope.launch(Dispatchers.IO) {
            val id = currentDraftId ?: store.createDraft().getOrNull()
            if (id != null) {
                currentDraftId = id
                s.bitmap?.let { bitmap ->
                    val annotated = android.graphics.Bitmap.createBitmap(
                        bitmap.width,
                        bitmap.height,
                        android.graphics.Bitmap.Config.ARGB_8888,
                    )
                    val canvas = android.graphics.Canvas(annotated)
                    canvas.drawBitmap(bitmap.asAndroidBitmap(), 0f, 0f, null)
                    val paint = android.graphics.Paint().apply {
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 4f
                        color = 0xFF6750A4.toInt()
                    }
                    s.rectangles.forEach { rectangle ->
                        canvas.drawRect(
                            rectangle.left * bitmap.width,
                            rectangle.top * bitmap.height,
                            rectangle.right * bitmap.width,
                            rectangle.bottom * bitmap.height,
                            paint,
                        )
                    }
                    val bytes = ByteArrayOutputStream().use { stream ->
                        annotated.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                        stream.toByteArray()
                    }
                    annotated.recycle()
                    store.writeAnnotated(id, bytes)
                }
                store.readDraft(id).getOrNull()?.let { manifest ->
                    store.writeManifest(id, manifest.copy(captureState = "Annotated"))
                }
            }
            withContext(Dispatchers.Main) { onSave(rect, s.feedbackText) }
        }
    }

    // --- Private helpers ---

    private fun createPlaceholderState(): EditorState =
        EditorState(bitmap = generatePlaceholderBitmap())

    private fun loadBitmapFromDraft(draftId: DraftId): ImageBitmap? {
        val path = store.directory.originalImagePath(draftId).toFile()
        if (!path.exists()) return null
        val bytes = runCatching { path.readBytes() }.getOrNull() ?: return null
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
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
