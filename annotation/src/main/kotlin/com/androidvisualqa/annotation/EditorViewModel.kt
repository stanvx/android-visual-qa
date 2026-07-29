package com.androidvisualqa.annotation

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.androidvisualqa.files.AtomicFileWriter
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.files.FileSystemDraftStore
import com.androidvisualqa.files.DraftDirectory
import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.model.annotation.AnnotationTool
import com.androidvisualqa.model.serialization.JsonConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import java.util.Base64
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
            val persisted = draftId?.let(::loadPersistedEditorState)
            _state.value = EditorState(
                bitmap = bitmap,
                annotations = persisted?.annotations.orEmpty(),
                rectangles = persisted?.rectangles.orEmpty(),
                feedbackText = persisted?.feedbackText.orEmpty(),
            )
        }
    }

    public fun addRectangle(rect: RectangleAnnotation) {
        // Guard: prevent undo-stack accumulation of null-bitmap states
        if (_state.value.bitmap == null) return
        val current = _state.value
        _state.value = current.commit(
            current.withAnnotations(current.editableAnnotations() + rect.toAnnotationItem())
                .copy(isDragging = false),
        )
        persistState()
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
        persistState()
    }

    public fun setDragging(dragging: Boolean) {
        _state.value = _state.value.copy(isDragging = dragging)
    }

    public fun setSnapCandidates(candidates: List<NormalizedBounds>) {
        _state.value = _state.value.withSnapCandidates(candidates)
    }

    /** Accepts a complete editor snapshot and persists it for process death. */
    public fun replaceState(state: EditorState) {
        _state.value = state
        persistState()
    }

    /**
     * Persists the annotated bitmap, then invokes [onSave] with the rectangle
     * and feedback text so the app can finish the report.
     */
    public fun save(onSave: (RectangleAnnotation?, String) -> Unit) {
        saveAnnotations { items, text -> onSave(items.lastOrNull()?.toRectangleOrNull(), text) }
    }

    public fun saveAnnotations(onSave: (List<AnnotationItem>, String) -> Unit) {
        val s = _state.value
        val items = s.editableAnnotations()

        viewModelScope.launch(Dispatchers.IO) {
            val id = currentDraftId ?: store.createDraft().getOrNull()
            if (id != null) {
                currentDraftId = id
                AtomicFileWriter.writeTextAtomically(
                    store.directory.editorStatePath(id),
                    EditorDraftStateCodec.encode(s),
                ).getOrElse { return@launch }
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
                    items.forEach { item ->
                        when (val geometry = item.geometry) {
                            is AnnotationGeometry.Lasso,
                            is AnnotationGeometry.Freehand,
                            -> {
                                val points = when (geometry) {
                                    is AnnotationGeometry.Lasso -> geometry.points
                                    is AnnotationGeometry.Freehand -> geometry.points
                                    else -> emptyList()
                                }
                                if (points.size > 1) {
                                    val path = android.graphics.Path().apply {
                                        moveTo(points.first().x * bitmap.width, points.first().y * bitmap.height)
                                        points.drop(1).forEach { point ->
                                            lineTo(point.x * bitmap.width, point.y * bitmap.height)
                                        }
                                        if (geometry is AnnotationGeometry.Lasso) close()
                                    }
                                    canvas.drawPath(path, paint)
                                }
                            }
                            else -> item.toRectangleOrNull()?.let { bounds ->
                                canvas.drawRect(
                                    bounds.left * bitmap.width,
                                    bounds.top * bitmap.height,
                                    bounds.right * bitmap.width,
                                    bounds.bottom * bitmap.height,
                                    paint,
                                )
                            }
                        }
                    }
                    val bytes = ByteArrayOutputStream().use { stream ->
                        annotated.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                        stream.toByteArray()
                    }
                    annotated.recycle()
                    store.writeAnnotated(id, bytes).getOrElse { return@launch }
                    val annotatedSha256 = com.androidvisualqa.files.Hashing.sha256(bytes)
                    store.readDraft(id).getOrNull()?.let { manifest ->
                        store.writeManifest(
                            id,
                            manifest.copy(
                                annotatedSha256 = annotatedSha256,
                                captureState = "Annotated",
                            ),
                        ).getOrElse { return@launch }
                    }
                }
            }
            withContext(Dispatchers.Main) { onSave(items, s.feedbackText) }
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

    private fun loadPersistedEditorState(draftId: DraftId): PersistedEditorState? {
        val statePath = store.directory.editorStatePath(draftId).toFile()
        if (statePath.exists()) {
            runCatching { EditorDraftStateCodec.decode(statePath.readText()) }.getOrNull()?.let { return it }
        }

        val reportPath = store.directory.reportJsonPath(draftId).toFile()
        if (!reportPath.exists()) return null
        return runCatching {
            val report = JsonConfig.decodeFromString<VisualFeedbackReport>(reportPath.readText())
            PersistedEditorState(
                annotations = report.annotations
                    .filter { it.toolType == AnnotationTool.Rectangle }
                    .map {
                        RectangleAnnotation(
                            id = AnnotationId(it.annotationId),
                            left = it.boundingBoxLeft.toFloat(),
                            top = it.boundingBoxTop.toFloat(),
                            right = it.boundingBoxRight.toFloat(),
                            bottom = it.boundingBoxBottom.toFloat(),
                            color = DEFAULT_ANNOTATION_COLOR,
                        ).toAnnotationItem().copy(
                            comments = it.linkedComments.map { comment ->
                                AnnotationComment(comment.commentId, comment.text)
                            },
                        )
                    },
                feedbackText = report.feedback.textBody.orEmpty(),
            )
        }.getOrNull()
    }

    private fun persistState() {
        val id = currentDraftId ?: return
        val snapshot = _state.value
        viewModelScope.launch(Dispatchers.IO) {
            AtomicFileWriter.writeTextAtomically(
                store.directory.editorStatePath(id),
                EditorDraftStateCodec.encode(snapshot),
            )
        }
    }

    private fun generatePlaceholderBitmap(): ImageBitmap {
        // 480×800 placeholder — solid light-grey surface
        val cfg = android.graphics.Bitmap.Config.ARGB_8888
        val bmp = android.graphics.Bitmap.createBitmap(480, 800, cfg)
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawColor(android.graphics.Color.argb(255, 230, 230, 230))
        return bmp.asImageBitmap()
    }

    private companion object {
        const val DEFAULT_ANNOTATION_COLOR = 0xFF6750A4L
    }
}

internal data class PersistedEditorState(
    val annotations: List<AnnotationItem>,
    val feedbackText: String,
) {
    val rectangles: List<RectangleAnnotation> get() = annotations.mapNotNull { it.toRectangleOrNull() }
}

internal object EditorDraftStateCodec {
    fun encode(state: EditorState): String = buildString {
        appendLine("feedback=${encodeText(state.feedbackText)}")
        state.editableAnnotations().forEach { item -> appendLine(encodeItem(item)) }
    }

    fun decode(text: String): PersistedEditorState {
        var feedbackText = ""
        val annotations = text.lineSequence().mapNotNull { line ->
            when {
                line.startsWith("feedback=") -> {
                    feedbackText = decodeText(line.removePrefix("feedback="))
                    null
                }
                line.startsWith("rectangle=") -> decodeLegacyRectangle(line.removePrefix("rectangle="))?.toAnnotationItem()
                line.startsWith("item=") -> decodeItem(line.removePrefix("item="))
                else -> null
            }
        }.toList()
        return PersistedEditorState(annotations, feedbackText)
    }

    private fun encodeItem(item: AnnotationItem): String {
        val comments = item.comments.joinToString(";") { "${encodeText(it.id)}:${encodeText(it.text)}" }
        val geometry = when (val value = item.geometry) {
            is AnnotationGeometry.Region -> "region,${value.bounds.left},${value.bounds.top},${value.bounds.right},${value.bounds.bottom}"
            is AnnotationGeometry.Lasso -> "lasso,${value.bounds.left},${value.bounds.top},${value.bounds.right},${value.bounds.bottom}," +
                value.points.joinToString(";") { "${it.x}:${it.y}" }
            is AnnotationGeometry.CommentMarker -> "marker,${value.position.x},${value.position.y}"
            is AnnotationGeometry.TextNote -> "text,${value.position.x},${value.position.y},${encodeText(value.text)}"
            is AnnotationGeometry.Arrow -> "arrow,${value.start.x},${value.start.y},${value.end.x},${value.end.y}"
            is AnnotationGeometry.Freehand -> "pen," + value.points.joinToString(";") { "${it.x}:${it.y}" }
        }
        return "item=${encodeText(item.id.value)}|${item.color}|$geometry|$comments"
    }

    private fun decodeItem(value: String): AnnotationItem? = runCatching {
        val fields = value.split('|', limit = 4)
        if (fields.size < 3) return null
        val geometryFields = fields[2].split(',')
        val geometry = when (geometryFields.first()) {
            "region" -> AnnotationGeometry.Region(NormalizedBounds.from(
                geometryFields[1].toFloat(), geometryFields[2].toFloat(),
                geometryFields[3].toFloat(), geometryFields[4].toFloat(),
            ))
            "lasso" -> AnnotationGeometry.Lasso(
                points = geometryFields.drop(5).joinToString(",").split(';').mapNotNull { point ->
                    point.split(':').takeIf { it.size == 2 }?.let {
                        NormalizedPoint.from(it[0].toFloat(), it[1].toFloat())
                    }
                },
                bounds = NormalizedBounds.from(
                    geometryFields[1].toFloat(), geometryFields[2].toFloat(),
                    geometryFields[3].toFloat(), geometryFields[4].toFloat(),
                ),
            )
            "marker" -> AnnotationGeometry.CommentMarker(
                NormalizedPoint.from(geometryFields[1].toFloat(), geometryFields[2].toFloat()),
            )
            "text" -> AnnotationGeometry.TextNote(
                NormalizedPoint.from(geometryFields[1].toFloat(), geometryFields[2].toFloat()),
                decodeText(geometryFields[3]),
            )
            "arrow" -> AnnotationGeometry.Arrow(
                NormalizedPoint.from(geometryFields[1].toFloat(), geometryFields[2].toFloat()),
                NormalizedPoint.from(geometryFields[3].toFloat(), geometryFields[4].toFloat()),
            )
            "pen" -> AnnotationGeometry.Freehand(
                geometryFields.drop(1).joinToString(",").split(';').mapNotNull { point ->
                    point.split(':').takeIf { it.size == 2 }?.let {
                        NormalizedPoint.from(it[0].toFloat(), it[1].toFloat())
                    }
                },
            )
            else -> return null
        }
        val comments = fields.getOrNull(3).orEmpty().split(';').filter { it.isNotBlank() }.mapNotNull { comment ->
            comment.split(':', limit = 2).takeIf { it.size == 2 }?.let {
                AnnotationComment(decodeText(it[0]), decodeText(it[1]))
            }
        }
        AnnotationItem(AnnotationId(decodeText(fields[0])), geometry, comments, fields[1].toLong())
    }.getOrNull()

    private fun decodeLegacyRectangle(value: String): RectangleAnnotation? = runCatching {
        val fields = value.split(',').takeIf { it.size == 6 } ?: return null
        RectangleAnnotation(
            id = AnnotationId(decodeText(fields[0])),
            left = fields[1].toFloat(), top = fields[2].toFloat(),
            right = fields[3].toFloat(), bottom = fields[4].toFloat(), color = fields[5].toLong(),
        )
    }.getOrNull()

    private fun encodeText(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decodeText(value: String): String =
        String(Base64.getDecoder().decode(value), Charsets.UTF_8)
}
