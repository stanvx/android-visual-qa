package com.androidvisualqa.privacy

import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.ids.NodeId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SensitiveFieldClassifierTest {

    private val classifier = SensitiveFieldClassifier()

    // -- Node classification --

    @Test
    fun `password node returns Credentials`() {
        val node = NodeSnapshot(
            nodeId = NodeId("n1"),
            isPassword = true,
            className = "android.widget.EditText",
        )
        assertEquals(Sensitivity.Credentials, classifier.classifyNode(node))
    }

    @Test
    fun `email view-id returns Pii`() {
        val node = NodeSnapshot(
            nodeId = NodeId("n1"),
            viewIdRaw = "com.example:id/email_address",
        )
        assertEquals(Sensitivity.Pii, classifier.classifyNode(node))
    }

    @Test
    fun `card view-id returns Financial`() {
        val node = NodeSnapshot(
            nodeId = NodeId("n1"),
            viewIdRaw = "com.example:id/credit_card_number",
        )
        assertEquals(Sensitivity.Financial, classifier.classifyNode(node))
    }

    @Test
    fun `EditText class without password returns Pii`() {
        val node = NodeSnapshot(
            nodeId = NodeId("n1"),
            className = "android.widget.EditText",
            isPassword = false,
        )
        assertEquals(Sensitivity.Pii, classifier.classifyNode(node))
    }

    @Test
    fun `benign node returns Public`() {
        val node = NodeSnapshot(
            nodeId = NodeId("n1"),
            className = "android.widget.Button",
            text = "Submit",
        )
        assertEquals(Sensitivity.Public, classifier.classifyNode(node))
    }

    // -- Feedback classification --

    @Test
    fun `feedback with email returns Pii`() {
        assertEquals(Sensitivity.Pii, classifier.classifyFeedback("my email is a@b.com"))
    }

    @Test
    fun `feedback with SSN returns Pii`() {
        assertEquals(Sensitivity.Pii, classifier.classifyFeedback("SSN: 123-45-6789"))
    }

    @Test
    fun `feedback with credit-card-like number returns Financial`() {
        assertEquals(Sensitivity.Financial, classifier.classifyFeedback("card: 4111-1111-1111-1111"))
    }

    @Test
    fun `feedback with API-key-like run returns Credentials`() {
        assertEquals(Sensitivity.Credentials, classifier.classifyFeedback("sk-proj-abcdef1234567890abcdef1234567890abcdef1234"))
    }

    @Test
    fun `benign feedback returns Public`() {
        assertEquals(Sensitivity.Public, classifier.classifyFeedback("This app looks great!"))
    }

    // -- matchedFieldNames --

    @Test
    fun `matchedFieldNames returns email and phone for mixed feedback`() {
        val names = classifier.matchedFieldNames("email me at a@b.com or call 555-123-4567")
        org.junit.jupiter.api.Assertions.assertTrue(names.contains("email"))
        org.junit.jupiter.api.Assertions.assertTrue(names.contains("phone"))
    }
}
