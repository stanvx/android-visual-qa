package com.androidvisualqa.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for persisted report metadata.
 *
 * Report content lives in the versioned JSON file on disk; Room stores the fields
 * needed for listing, filtering, status and retention queries.
 *
 * All columns use Room's default snake_case naming (e.g. `report_id`, `created_at`).
 * DAO queries reference Kotlin property names; Room resolves them at compile time.
 */
@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val reportId: String,
    val draftId: String? = null,
    val createdAt: Long, // epoch millis
    val status: String,      // ReportStatus enum name
    val packageName: String,
    val lastStateName: String,
    val schemaVersion: Int,
    val reportJsonPath: String,          // relative to drafts root
    val annotatedPngPath: String,      // relative to drafts root
    val sha256: String,
    val feedbackText: String? = null,
    val secureWindowResult: String? = null,
)
