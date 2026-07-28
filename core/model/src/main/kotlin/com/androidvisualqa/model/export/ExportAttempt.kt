package com.androidvisualqa.model.export

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Record of a single export attempt for a report.
 *
 * @property exportId Unique identifier for this attempt.
 * @property destination Descriptor (e.g. "share_sheet", "filesystem", "github:issue").
 * @property startedAt When the export began.
 * @property completedAt When the export finished (null if still running).
 * @property success Whether the export completed successfully.
 * @property failureReason Error details if the export failed.
 * @property exportedFormats List of formats produced (e.g. "json", "md", "zip").
 */
@Serializable
data class ExportAttempt(
    val exportId: String,
    val destination: String,
    val startedAt: Instant,
    val completedAt: Instant? = null,
    val success: Boolean = false,
    val failureReason: String? = null,
    val exportedFormats: List<String> = emptyList(),
)
