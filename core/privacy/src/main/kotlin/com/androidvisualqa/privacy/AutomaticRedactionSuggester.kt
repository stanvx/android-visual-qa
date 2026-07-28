package com.androidvisualqa.privacy

import com.androidvisualqa.model.capture.NodeSnapshot

/**
 * Suggests redaction regions based on automated classification of nodes and feedback text.
 *
 * The suggester is **deterministic**: same inputs always produce the same output.
 *
 * ## Node-based suggestions
 *
 * Nodes classified as [Sensitivity.Credentials] or [Sensitivity.Financial] produce
 * one [RedactionRegion] each, with bounds normalized against the provided canvas
 * dimensions. Nodes at [Sensitivity.Pii] are surfaced to the user but not auto-redacted
 * — the M3 design treats PII as "suggest but require user confirmation."
 *
 * ## Feedback-based suggestions
 *
 * If the feedback text contains any sensitive match, a single [RedactionRegion]
 * covering the full captured frame `(0.0, 0.0, 1.0, 1.0)` is returned as a
 * conservative assumption. M4 may refine this with actual editor coordinates.
 */
class AutomaticRedactionSuggester(
    private val classifier: SensitiveFieldClassifier = SensitiveFieldClassifier(),
) {

    /**
     * Produces a deterministic list of suggested redaction regions.
     *
     * @param nodes The flattened accessibility tree nodes.
     * @param feedback The free-form feedback text.
     * @param canvasWidth Pixel width of the captured frame, used to normalise node bounds.
     * @param canvasHeight Pixel height of the captured frame, used to normalise node bounds.
     * @return An ordered list of suggested [RedactionRegion]s.
     */
    fun suggest(
        nodes: List<NodeSnapshot>,
        feedback: String,
        canvasWidth: Int = 1080,
        canvasHeight: Int = 1920,
    ): List<RedactionRegion> {
        val regions = mutableListOf<RedactionRegion>()

        val w = canvasWidth.toDouble()
        val h = canvasHeight.toDouble()

        // Node-based suggestions: auto-redact Credentials and Financial only.
        for (node in nodes) {
            val sensitivity = classifier.classifyNode(node)
            if (sensitivity == Sensitivity.Credentials || sensitivity == Sensitivity.Financial) {
                val left = node.boundsLeft.toDouble() / w
                val top = node.boundsTop.toDouble() / h
                val right = node.boundsRight.toDouble() / w
                val bottom = node.boundsBottom.toDouble() / h
                regions.add(
                    RedactionRegion(
                        left = left.coerceIn(0.0, 1.0),
                        top = top.coerceIn(0.0, 1.0),
                        right = right.coerceIn(0.0, 1.0),
                        bottom = bottom.coerceIn(0.0, 1.0),
                        sensitivity = sensitivity,
                        reason = "Auto-detected ${sensitivity.wire} in node ${node.className ?: "unknown"}"
                    )
                )
            }
        }

        // Feedback-based suggestion: full-frame conservative guess
        val feedbackSensitivity = classifier.classifyFeedback(feedback)
        if (feedbackSensitivity != Sensitivity.Public) {
            regions.add(
                RedactionRegion(
                    left = 0.0,
                    top = 0.0,
                    right = 1.0,
                    bottom = 1.0,
                    sensitivity = feedbackSensitivity,
                    reason = "Auto-detected ${feedbackSensitivity.wire} in feedback text"
                )
            )
        }

        return regions
    }
}
