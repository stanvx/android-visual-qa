package com.androidvisualqa.export.agent

import com.androidvisualqa.model.serialization.JsonConfig
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AgentBundleSerializationTest {

    @Test
    fun `round-trip through JSON preserves all fields`() {
        val original = AgentBundle(
            reportId = "r-1",
            createdAt = "2025-07-29T12:00:00Z",
            packageName = "com.example",
            windowId = 42,
            feedback = "Button too small",
            annotations = listOf(
                AgentAnnotation(
                    id = "ann-1",
                    toolType = "Rectangle",
                    boundsNormalized = AgentBounds(0.1, 0.2, 0.5, 0.4),
                    color = "#FFFF0000",
                ),
            ),
            candidates = listOf(
                AgentCandidate(
                    selectionId = "sel-1",
                    choiceType = "AutoSelected",
                    confidence = 0.92,
                    nodeId = "node-1",
                    sdkComponentId = "btn_submit",
                    explanation = "Overlap match",
                ),
            ),
            sdkComponents = emptyList(),
            privacy = AgentPrivacy(
                secureWindowResult = "NotSecure",
                excludedFields = listOf("secret"),
                redactionCount = 0,
            ),
            instructions = listOf("Open the report.md"),
            rawReportJsonPath = "report.json",
            originalPngPath = "original.png",
            annotatedPngPath = "annotated.png",
        )

        val json = JsonConfig.encodeToString(original)
        assertNotNull(json)
        assertFalse(json.contains("kotlin"), "JSON must not contain Kotlin class names")

        val decoded = JsonConfig.decodeFromString<AgentBundle>(json)
        assertEquals(original, decoded)
    }

    @Test
    fun `empty lists serialize correctly`() {
        val bundle = AgentBundle(
            reportId = "r-2",
            createdAt = "2025-07-29T12:00:00Z",
            packageName = "com.empty",
            windowId = null,
            feedback = "",
            annotations = emptyList(),
            candidates = emptyList(),
            sdkComponents = emptyList(),
            privacy = AgentPrivacy(
                secureWindowResult = "Unknown",
                excludedFields = emptyList(),
                redactionCount = 0,
            ),
            instructions = emptyList(),
        )
        val json = JsonConfig.encodeToString(bundle)
        assertNotNull(json)
        assertTrue(json.contains("\"annotations\":"))
        assertTrue(json.contains("\"candidates\":"))
    }

    @Test
    fun `default schema version serializes`() {
        val bundle = AgentBundle(
            reportId = "r-3",
            createdAt = "2025-07-29T12:00:00Z",
            packageName = "com.default",
            windowId = null,
            feedback = "",
            annotations = emptyList(),
            candidates = emptyList(),
            sdkComponents = emptyList(),
            privacy = AgentPrivacy(
                secureWindowResult = "NotSecure",
                excludedFields = emptyList(),
                redactionCount = 0,
            ),
            instructions = emptyList(),
        )
        val json = JsonConfig.encodeToString(bundle)
        assertEquals(1, bundle.schemaVersion)
        assertTrue(json.contains("\"schemaVersion\":1"))
    }

    private fun assertTrue(condition: Boolean, message: String? = null) {
        org.junit.jupiter.api.Assertions.assertTrue(condition, message)
    }
}
