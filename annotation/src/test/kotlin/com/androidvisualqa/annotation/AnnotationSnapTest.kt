package com.androidvisualqa.annotation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnnotationSnapTest {
    @Test
    fun `nearby entity wins and edges clamp`() {
        val snapped = snapRegion(
            NormalizedBounds.from(0.01f, 0.12f, 0.49f, 0.48f),
            listOf(NormalizedBounds.from(0.03f, 0.1f, 0.5f, 0.5f)),
        )

        assertEquals(NormalizedBounds.from(0.03f, 0.1f, 0.5f, 0.5f), snapped)
        assertEquals(
            NormalizedBounds.from(0f, 0.12f, 0.49f, 1f),
            snapRegion(NormalizedBounds.from(0.01f, 0.12f, 0.49f, 0.99f), emptyList()),
        )
    }
}
