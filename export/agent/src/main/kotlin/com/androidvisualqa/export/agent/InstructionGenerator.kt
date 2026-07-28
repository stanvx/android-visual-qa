package com.androidvisualqa.export.agent

import com.androidvisualqa.model.VisualFeedbackReport

/**
 * Produces a deterministic list of suggested next steps for an AI agent
 * based on the contents of a [VisualFeedbackReport].
 *
 * Heuristics are cheap and purely heuristic — they do not perform any
 * external analysis. Same input always produces the same output.
 */
object InstructionGenerator {

    /**
     * Generate suggested instructions for an AI agent.
     *
     * The returned list is built by evaluating report contents against a
     * small set of heuristics. The order is stable.
     */
    fun generate(report: VisualFeedbackReport): List<String> {
        val instructions = mutableListOf<String>()

        // ── Crash / exception detection ──────────────────────────────────
        val feedbackText = report.feedback.textBody.orEmpty().lowercase()
        if (feedbackText.contains("crash") ||
            feedbackText.contains("exception") ||
            feedbackText.contains("stacktrace")
        ) {
            instructions.add(
                "Read the original.png to inspect the error UI; " +
                    "consider opening the issue as a bug."
            )
        }

        // ── High-confidence SDK-matched selections ────────────────────────
        val highConfidence = report.selections.filter {
            it.confidence >= 0.9 && it.chosenSdkComponentId != null
        }
        for (selection in highConfidence) {
            val sdkId = selection.chosenSdkComponentId!!.value
            instructions.add(
                "The selected component is `$sdkId` at the associated route — " +
                    "likely root cause."
            )
        }

        // ── Empty feedback ───────────────────────────────────────────────
        if (feedbackText.isBlank()) {
            instructions.add("Ask the user for a clearer description of the issue.")
        }

        // ── Redactions present ───────────────────────────────────────────
        val totalRedactions =
            report.privacy.automaticRedactions.size + report.privacy.userRedactions.size
        if (totalRedactions > 0) {
            instructions.add(
                "Verify with the user that the redacted regions are acceptable " +
                    "before sharing externally."
            )
        }

        // ── Default / fallback ───────────────────────────────────────────
        if (instructions.isEmpty()) {
            instructions.add("Open the report.md in a markdown viewer for full context.")
        }

        return instructions
    }
}
