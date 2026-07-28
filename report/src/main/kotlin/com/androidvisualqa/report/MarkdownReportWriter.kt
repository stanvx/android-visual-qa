package com.androidvisualqa.report

import com.androidvisualqa.files.AtomicFileWriter
import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.model.annotation.AnnotationEvidence
import com.androidvisualqa.model.attachment.AttachmentRef
import com.androidvisualqa.model.selection.ComponentSelection
import java.nio.file.Path

/**
 * Generates a human-readable Markdown representation of a [VisualFeedbackReport].
 */
public class MarkdownReportWriter {

    private fun renderAnnotations(annotations: List<AnnotationEvidence>): String {
        if (annotations.isEmpty()) return ""
        return annotations.joinToString("\n") { a ->
            "- tool=${a.toolType.name} id=${a.annotationId} bounds=(${a.boundingBoxLeft},${a.boundingBoxTop},${a.boundingBoxRight},${a.boundingBoxBottom}) color=#00000000 stroke=0px"
        }
    }

    private fun renderSelections(selections: List<ComponentSelection>): String {
        if (selections.isEmpty()) return ""
        return selections.joinToString("\n") { s ->
            val explanation = s.chosenNodeId?.value ?: s.chosenSdkComponentId?.value ?: "none"
            "- choice=${s.choiceType.name} confidence=${"%.2f".format(s.confidence)} nodeId=${explanation} explanation=${explanation}"
        }
    }

    private fun renderAttachments(attachments: List<AttachmentRef>): String {
        if (attachments.isEmpty()) return ""
        return attachments.joinToString("\n") { a ->
            "- id=${a.attachmentId.value} mime=${a.mimeType} sha256=${a.sha256Hex} path=${a.fileName}"
        }
    }

    /**
     * Generates a Markdown document from [report].
     */
    public fun write(report: VisualFeedbackReport): String {
        val frame = report.frame
        val privacy = report.privacy

        return buildString {
            appendLine("# Visual Feedback Report")
            appendLine()
            appendLine("- **Report ID:** ${report.reportId.value}")
            appendLine("- **Created:** ${report.createdAt}")
            appendLine("- **Status:** ${report.status.name}")
            appendLine("- **Schema version:** ${report.schemaVersion}")
            appendLine("- **Package:** ${frame.packageName}")
            appendLine("- **Window ID:** ${frame.windowId}")
            appendLine("- **Display:** ${frame.displayId}")
            appendLine("- **Capture state:** ${report.capture.lastStateName}")
            appendLine()
            appendLine("## Feedback")
            appendLine(report.feedback.textBody ?: "(no text)")
            appendLine()
            appendLine("## Annotations")
            val annotationsMd = renderAnnotations(report.annotations)
            if (annotationsMd.isNotEmpty()) {
                appendLine(annotationsMd)
            }
            appendLine()
            appendLine("## Component Selections")
            val selectionsMd = renderSelections(report.selections)
            if (selectionsMd.isNotEmpty()) {
                appendLine(selectionsMd)
            }
            appendLine()
            appendLine("## Privacy")
            appendLine("- Secure window: ${privacy.secureWindowResult.name}")
            appendLine("- Redactions: ${privacy.automaticRedactions.size + privacy.userRedactions.size}")
            appendLine("- Classifications: ${privacy.excludedFields.size}")
            appendLine()
            appendLine("## Attachments")
            val attachmentsMd = renderAttachments(report.attachments)
            if (attachmentsMd.isNotEmpty()) {
                appendLine(attachmentsMd)
            }
        }
    }

    /**
     * Atomically writes the Markdown document to [path] via [AtomicFileWriter].
     */
    public suspend fun write(report: VisualFeedbackReport, path: Path): Result<Unit> {
        val content = write(report)
        return AtomicFileWriter.writeTextAtomically(path, content)
    }
}
