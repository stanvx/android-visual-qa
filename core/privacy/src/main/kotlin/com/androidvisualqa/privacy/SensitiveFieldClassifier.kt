package com.androidvisualqa.privacy

import com.androidvisualqa.model.capture.NodeSnapshot

/**
 * Heuristic classifier for sensitive content in accessibility nodes and free-text feedback.
 *
 * This is a v1 rule-based classifier. Future milestones (M4+) may replace or augment
 * the heuristics with ML-based classification.
 *
 * False-positive risks: the API key pattern matches any 32+ alphanumeric token (including
 * non-secret IDs). v1 accepts this; M4 ML classification should narrow it.
 *
 * ## Node classification heuristics
 *
 * 1. `isPassword == true` → [Sensitivity.Credentials].
 * 2. `className` matches `.*EditText.*` with password-relevant content →
 *    [Sensitivity.Credentials]; otherwise [Sensitivity.Pii] for text input fields.
 * 3. `viewIdResourceName` (as [viewIdRaw]) matched case-insensitively against known
 *    sensitive substrings → [Sensitivity.Pii] or [Sensitivity.Financial].
 * 4. All other nodes → [Sensitivity.Public].
 *
 * ## Feedback text heuristics
 *
 * Each pattern is checked independently; the highest-severity match wins
 * (Credentials > Financial > Pii > Public).
 *
 * | Pattern | Sensitivity |
 * |---|---|
 * | Email: `\b\S+@\S+\.\S+\b` | Pii |
 * | Phone: `\b\d{3}[\s-]?\d{3}[\s-]?\d{4}\b` | Pii |
 * | SSN: `\b\d{3}-\d{2}-\d{4}\b` | Pii |
 * | Credit card: 13–19 consecutive digits or grouped digits | Financial |
 * | API key: 32+ alphanumeric characters | Credentials |
 */
class SensitiveFieldClassifier {

    private val emailRegex = Regex("""\b\S+@\S+\.\S+\b""")
    private val phoneRegex = Regex("""\b\d{3}[\s-]?\d{3}[\s-]?\d{4}\b""")
    private val ssnRegex = Regex("""\b\d{3}-\d{2}-\d{4}\b""")
    private val cardRegex = Regex("""\b(?:\d{4}[-\s]?){3}\d{4}\b""")
    private val apiKeyRegex = Regex("""\b[A-Za-z0-9]{32,}\b""")

    private val piiViewIdPatterns = listOf(
        "email", "phone", "ssn", "tax"
    )
    private val financialViewIdPatterns = listOf(
        "account", "card", "cvv", "pin"
    )

    /**
     * Classifies an accessibility [NodeSnapshot] based on its metadata.
     *
     * @return The inferred [Sensitivity]. Defaults to [Sensitivity.Public].
     */
    fun classifyNode(node: NodeSnapshot): Sensitivity {
        // 1. Explicit password flag
        if (node.isPassword) return Sensitivity.Credentials

        // 2. Class-name based heuristic for text inputs
        val className = node.className?.lowercase() ?: ""
        val isEditText = className.contains("edittext") ||
            className.contains("edit_text") ||
            className.contains("edit-text")

        if (isEditText) {
            // Already handled password fields above, so remaining EditText nodes are Pii
            return Sensitivity.Pii
        }

        // 3. View-ID heuristics
        val viewId = node.viewIdRaw?.lowercase() ?: ""
        if (viewId.isNotEmpty()) {
            for (pattern in financialViewIdPatterns) {
                if (viewId.contains(pattern)) return Sensitivity.Financial
            }
            for (pattern in piiViewIdPatterns) {
                if (viewId.contains(pattern)) return Sensitivity.Pii
            }
        }

        // 4. Content-description heuristics (lightweight)
        val contentDesc = node.contentDescription?.lowercase() ?: ""
        val text = node.text?.lowercase() ?: ""
        val combined = "$contentDesc $text"
        for (pattern in financialViewIdPatterns) {
            if (combined.contains(pattern)) return Sensitivity.Financial
        }
        for (pattern in piiViewIdPatterns) {
            if (combined.contains(pattern)) return Sensitivity.Pii
        }

        return Sensitivity.Public
    }

    /**
     * Classifies free-form feedback [text].
     *
     * @return The highest-severity [Sensitivity] found. Returns [Sensitivity.Public] if no pattern matches.
     */
    fun classifyFeedback(text: String): Sensitivity {
        // Highest severity first
        if (apiKeyRegex.containsMatchIn(text)) return Sensitivity.Credentials
        if (cardRegex.containsMatchIn(text)) return Sensitivity.Financial
        if (ssnRegex.containsMatchIn(text)) return Sensitivity.Pii
        if (emailRegex.containsMatchIn(text)) return Sensitivity.Pii
        if (phoneRegex.containsMatchIn(text)) return Sensitivity.Pii
        return Sensitivity.Public
    }

    /**
     * Returns the set of human-readable field names matched in [text].
     * Used to populate [com.androidvisualqa.model.privacy.PrivacyEvidence.excludedFields].
     */
    fun matchedFieldNames(text: String): List<String> {
        val names = mutableListOf<String>()
        if (emailRegex.containsMatchIn(text)) names.add("email")
        if (phoneRegex.containsMatchIn(text)) names.add("phone")
        if (ssnRegex.containsMatchIn(text)) names.add("ssn")
        if (cardRegex.containsMatchIn(text)) names.add("credit_card")
        if (apiKeyRegex.containsMatchIn(text)) names.add("api_key")
        return names
    }
}
