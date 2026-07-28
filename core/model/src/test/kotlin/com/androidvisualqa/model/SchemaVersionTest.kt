package com.androidvisualqa.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SchemaVersionTest {

    @Test
    fun `CURRENT_SCHEMA_VERSION is 1`() {
        assertEquals(1, VisualFeedbackReport.CURRENT_SCHEMA_VERSION)
    }
}
