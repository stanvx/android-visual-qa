package com.androidvisualqa.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for process-death resumption of in-flight drafts.
 *
 * The Room row is the single source of truth for the last known state of a
 * capture transaction. On app restart, the launcher queries all draft states,
 * rehydrates the corresponding [DraftManifest] from disk, and resumes the
 * capture state machine from the recorded [lastStateName].
 *
 * @property pendingActions JSON-serialized list of [CaptureCommand] names that
 *   were queued but not yet processed (e.g. `["ContextReady","PixelsReady"]`).
 *   Format: a plain JSON string array — `[]` when idle.
 */
@Entity(tableName = "draft_states")
data class DraftStateEntity(
    @PrimaryKey val draftId: String,
    val lastStateName: String,
    val updatedAt: Long, // epoch millis
    val pendingActions: String, // JSON array of CaptureCommand names
)
