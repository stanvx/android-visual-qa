package com.androidvisualqa.model

import com.androidvisualqa.model.annotation.AnnotationComment
import com.androidvisualqa.model.annotation.AnnotationEvidence
import com.androidvisualqa.model.annotation.AnnotationTool
import com.androidvisualqa.model.annotation.NormalizedPoint
import com.androidvisualqa.model.serialization.JsonConfig
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnnotationEvidenceTest {

    @Test
    fun `multiple linked comments round trip with normalized geometry`() {
        val evidence = AnnotationEvidence(
            annotationId = "ann-1",
            toolType = AnnotationTool.Rectangle,
            strokePoints = listOf(NormalizedPoint(0.1, 0.2), NormalizedPoint(0.8, 0.9)),
            boundingBoxLeft = 0.1,
            boundingBoxTop = 0.2,
            boundingBoxRight = 0.8,
            boundingBoxBottom = 0.9,
            linkedComments = listOf(
                AnnotationComment("comment-1", "Move this control"),
                AnnotationComment("comment-2", "Keep the hit target size"),
            ),
        )

        val restored = JsonConfig.decodeFromString<AnnotationEvidence>(JsonConfig.encodeToString(evidence))

        assertEquals(evidence, restored)
        assertTrue(evidence.linkedComments.all { it.text.isNotBlank() })
    }
}
