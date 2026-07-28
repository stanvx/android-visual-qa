package com.androidvisualqa.export.share

import com.androidvisualqa.model.VisualFeedbackReport
import java.util.Arrays

/**
 * Pure data class holding a report and its pre-built binary artifacts.
 *
 * [equals] and [hashCode] compare byte arrays by content rather than
 * identity so that two instances wrapping identical bytes are equal.
 *
 * @property report     The visual feedback report.
 * @property zipBytes   Already-built ZIP bundle containing report.json, report.md,
 *                      original.png, annotated.png, and manifest.json.
 * @property originalPng   Raw bytes of the original screenshot PNG.
 * @property annotatedPng  Raw bytes of the annotated screenshot PNG.
 */
public data class ShareableReport(
    val report: VisualFeedbackReport,
    val zipBytes: ByteArray,
    val originalPng: ByteArray,
    val annotatedPng: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShareableReport) return false
        return report == other.report &&
            zipBytes.contentEquals(other.zipBytes) &&
            originalPng.contentEquals(other.originalPng) &&
            annotatedPng.contentEquals(other.annotatedPng)
    }

    override fun hashCode(): Int {
        var result = report.hashCode()
        result = 31 * result + zipBytes.contentHashCode()
        result = 31 * result + originalPng.contentHashCode()
        result = 31 * result + annotatedPng.contentHashCode()
        return result
    }
}
