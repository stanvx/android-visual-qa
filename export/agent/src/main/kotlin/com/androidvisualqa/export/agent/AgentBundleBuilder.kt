package com.androidvisualqa.export.agent

import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.model.attachment.AttachmentRef
import com.androidvisualqa.model.capture.SdkComponentSnapshot
import com.androidvisualqa.model.privacy.SecureWindowResult
import com.androidvisualqa.model.serialization.JsonConfig
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString

/**
 * Assembles an [AgentBundle] from a [VisualFeedbackReport].
 *
 * @param canvasWidth  Width of the original captured image in pixels.
 * @param canvasHeight Height of the original captured image in pixels.
 * @param clock        Clock used for any timestamps (default [Clock.System]).
 */
class AgentBundleBuilder(
    private val canvasWidth: Int,
    private val canvasHeight: Int,
    private val clock: Clock = Clock.System,
) {

    /**
     * Build an [AgentBundle] from the given report.
     *
     * All pixel bounds from the source report are normalised to [0.0, 1.0]
     * using the canvas dimensions supplied at construction.
     */
    fun build(report: VisualFeedbackReport): AgentBundle {
        val frame = report.frame

        val annotations = report.annotations.map { ann ->
            AgentAnnotation(
                id = ann.annotationId,
                toolType = ann.toolType.name,
                boundsNormalized = BoundsNormalizer.normalize(
                    left = ann.boundingBoxLeft,
                    top = ann.boundingBoxTop,
                    right = ann.boundingBoxRight,
                    bottom = ann.boundingBoxBottom,
                    canvasWidth = canvasWidth.toDouble(),
                    canvasHeight = canvasHeight.toDouble(),
                ),
                // ponytail: annotation evidence stores colour per-stroke-point
                //           rather than a single annotation colour.
                color = null,
            )
        }

        val candidates = report.selections.map { sel ->
            AgentCandidate(
                selectionId = sel.selectionId,
                choiceType = sel.choiceType.name,
                confidence = sel.confidence,
                nodeId = sel.chosenNodeId?.value,
                sdkComponentId = sel.chosenSdkComponentId?.value,
                // ponytail: the model has no single "explanation" field on
                // ComponentSelection; we derive a brief one from the scores.
                explanation = explainSelection(sel),
            )
        }

        val sdkComponents = buildSdkComponents(report)

        val privacy = AgentPrivacy(
            secureWindowResult = when (report.privacy.secureWindowResult) {
                SecureWindowResult.SecureWindow -> "Secure"
                SecureWindowResult.NotSecure -> "NotSecure"
                SecureWindowResult.Unknown -> "Unknown"
            },
            excludedFields = report.privacy.excludedFields.toList(),
            redactionCount = report.privacy.automaticRedactions.size +
                report.privacy.userRedactions.size,
        )

        // Look up attachment paths from the attachment refs
        val rawReportJsonPath = attachmentFileNameByRole(report.attachments, "report_json")
        val originalPngPath = attachmentFileNameByRole(report.attachments, "original_screenshot")
        val annotatedPngPath = attachmentFileNameByRole(report.attachments, "annotated_image")

        return AgentBundle(
            reportId = report.reportId.value,
            createdAt = report.createdAt.toString(),
            packageName = frame.packageName,
            windowId = if (frame.windowId != 0) frame.windowId.toLong() else null,
            feedback = report.feedback.textBody.orEmpty(),
            annotations = annotations,
            candidates = candidates,
            sdkComponents = sdkComponents,
            privacy = privacy,
            instructions = InstructionGenerator.generate(report),
            rawReportJsonPath = rawReportJsonPath,
            originalPngPath = originalPngPath,
            annotatedPngPath = annotatedPngPath,
        )
    }

    /**
     * Build an [AgentBundle] and serialise it to a compact JSON string.
     *
     * Uses the project-wide [JsonConfig] to avoid Kotlin class name leaks.
     */
    fun buildJson(report: VisualFeedbackReport): String {
        val bundle = build(report)
        return JsonConfig.encodeToString(bundle)
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Extract and normalise SDK component snapshots.
     *
     * ponytail: the current model stores [SdkComponentSnapshot] as part of
     * the accessibility node tree rather than a flat top-level list.  Once
     * the enrichment pipeline in M6+ provides a flat snapshot list this will
     * hydrate fully.  For now, return empty — the bundle contract is sound.
     */
    // TODO(m6): hydrate SdkComponentSnapshot from the report once the
    // enrichment pipeline provides a flat top-level snapshot list.
    private fun buildSdkComponents(report: VisualFeedbackReport): List<AgentSdkComponent> {
        return emptyList()
    }

    /**
     * Build a brief human-readable explanation from the selection scores.
     */
    private fun explainSelection(
        sel: com.androidvisualqa.model.selection.ComponentSelection,
    ): String {
        val parts = mutableListOf<String>()
        if (sel.scoreOverlap > 0.0) parts.add("overlap=%.2f".format(sel.scoreOverlap))
        if (sel.scoreContainment > 0.0) parts.add("containment=%.2f".format(sel.scoreContainment))
        if (sel.scoreCenterProximity > 0.0) parts.add("center=%.2f".format(sel.scoreCenterProximity))
        if (sel.confidence > 0.0) parts.add("confidence=%.2f".format(sel.confidence))
        return if (parts.isEmpty()) "NoMatch" else parts.joinToString("; ")
    }

    private fun attachmentFileNameByRole(
        attachments: List<AttachmentRef>,
        role: String,
    ): String? =
        attachments.firstOrNull { it.role == role }?.fileName
}
