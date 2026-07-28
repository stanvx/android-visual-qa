package com.androidvisualqa.model

import kotlinx.serialization.Serializable

/**
 * Lifecycle status of a [VisualFeedbackReport].
 *
 * Draft       — just captured, not yet annotated
 * Annotating  — user is adding strokes / selections
 * Reviewing   — user is reviewing before save/export
 * Saved       — persisted locally
 * Exported    — successfully exported to at least one destination
 * Failed      — terminal failure
 * Cancelled   — user aborted before completion
 */
@Serializable
enum class ReportStatus {
    Draft,
    Annotating,
    Reviewing,
    Saved,
    Exported,
    Failed,
    Cancelled,
}
