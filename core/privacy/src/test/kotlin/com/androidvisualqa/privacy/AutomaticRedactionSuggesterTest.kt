package com.androidvisualqa.privacy

import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.ids.NodeId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AutomaticRedactionSuggesterTest {

    private val suggester = AutomaticRedactionSuggester()

    @Test
    fun `password node and email feedback produce two suggestions`() {
        val nodes = listOf(
            NodeSnapshot(
                nodeId = NodeId("n1"),
                className = "android.widget.EditText",
                isPassword = true,
                boundsLeft = 100, boundsTop = 200,
                boundsRight = 400, boundsBottom = 250,
            )
        )
        val feedback = "my email is a@b.com"

        val suggestions = suggester.suggest(nodes, feedback, canvasWidth = 1080, canvasHeight = 1920)

        assertEquals(2, suggestions.size)
    }

    @Test
    fun `suggestions are deterministic for same inputs`() {
        val nodes = listOf(
            NodeSnapshot(
                nodeId = NodeId("n1"),
                className = "android.widget.EditText",
                isPassword = true,
                boundsLeft = 100, boundsTop = 200,
                boundsRight = 400, boundsBottom = 250,
            )
        )
        val feedback = "my email is a@b.com"

        val first = suggester.suggest(nodes, feedback, canvasWidth = 1080, canvasHeight = 1920)
        val second = suggester.suggest(nodes, feedback, canvasWidth = 1080, canvasHeight = 1920)

        assertEquals(first, second)
    }

    @Test
    fun `no sensitive nodes and public feedback returns empty list`() {
        val nodes = listOf(
            NodeSnapshot(
                nodeId = NodeId("n1"),
                className = "android.widget.Button",
                text = "Submit",
                boundsLeft = 0, boundsTop = 0,
                boundsRight = 100, boundsBottom = 50,
            )
        )
        val feedback = "This is fine."
        val suggestions = suggester.suggest(nodes, feedback, canvasWidth = 1080, canvasHeight = 1920)
        assertEquals(0, suggestions.size)
    }
}
