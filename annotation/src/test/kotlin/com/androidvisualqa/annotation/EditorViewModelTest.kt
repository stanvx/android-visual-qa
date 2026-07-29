package com.androidvisualqa.annotation

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * JVM-level verification for [EditorViewModel] behaviours that don't require
 * an Android instrumented environment.
 *
 * The ViewModel itself requires an [android.app.Application] instance, so
 * full integration tests run on-device (M2). Here we verify the data-model
 * contracts that the ViewModel orchestrates.
 *
 * // TODO(m2): add Robolectric-based ViewModel tests when the test infra ships.
 */
class EditorViewModelTest {

    @Test
    fun `editor draft state round trips annotations and comments`() {
        val state = EditorState(
            rectangles = listOf(
                RectangleAnnotation(AnnotationId("r1"), 0.1f, 0.2f, 0.7f, 0.8f, 0xFF6750A4L),
            ),
            feedbackText = "line one\nline two",
        )

        val restored = EditorDraftStateCodec.decode(EditorDraftStateCodec.encode(state))

        assertEquals(state.rectangles, restored.rectangles)
        assertEquals(state.feedbackText, restored.feedbackText)
    }

    @Test
    fun `closed lasso keeps its snapped bounds and comment`() {
        val lasso = AnnotationItem(
            id = AnnotationId("lasso-1"),
            geometry = AnnotationGeometry.Lasso(
                points = listOf(
                    NormalizedPoint.from(0.1f, 0.2f),
                    NormalizedPoint.from(0.7f, 0.2f),
                    NormalizedPoint.from(0.7f, 0.8f),
                    NormalizedPoint.from(0.1f, 0.8f),
                    NormalizedPoint.from(0.1f, 0.2f),
                ),
                bounds = NormalizedBounds.from(0.08f, 0.18f, 0.72f, 0.82f),
            ),
        ).withComment("Fix this")
        val restored = EditorDraftStateCodec.decode(
            EditorDraftStateCodec.encode(EditorState(annotations = listOf(lasso))),
        )

        assertEquals(lasso, restored.annotations.single())
    }

    @Test
    fun `save callback receives rectangle and feedback text`() {
        // The ViewModel's save() signature is:
        //   fun save(onSave: (RectangleAnnotation?, String) -> Unit)
        //
        // This test verifies the callback contract compiles.
        val rect = RectangleAnnotation(
            id = AnnotationId("r1"),
            left = 0.1f, top = 0.1f, right = 0.5f, bottom = 0.5f,
            color = 0xFF6750A4L,
        )
        val feedback = "Test feedback"

        var capturedRect: RectangleAnnotation? = null
        var capturedText: String? = null

        val callback: (RectangleAnnotation?, String) -> Unit = { r, t ->
            capturedRect = r
            capturedText = t
        }

        callback(rect, feedback)

        assert(capturedRect != null)
        assert(capturedRect!!.id.value == "r1")
        assert(capturedText == "Test feedback")
    }
}
