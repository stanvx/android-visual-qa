package com.androidvisualqa.app.resumption

import com.androidvisualqa.database.DraftStateDao
import com.androidvisualqa.database.DraftStateEntity
import com.androidvisualqa.files.DraftDirectory
import com.androidvisualqa.model.ids.DraftId
import kotlinx.coroutines.flow.first
import java.nio.file.Files

/**
 * Coordinates process-death resumption of in-flight drafts.
 *
 * On app restart after a process death, query [loadResumableDrafts] to get
 * the set of drafts that were in progress. Each draft's [DraftStateEntity]
 * records the last known capture state name and pending actions.
 *
 * To attempt resumption, call [resumeDraft] with a draft ID. If the
 * corresponding draft directory and files still exist on disk, the method
 * returns the [DraftId] so the caller can rehydrate the state machine. If
 * the files have been cleaned up (e.g. by retention), the database row is
 * deleted and null is returned.
 *
 * @param draftStateDao DAO for draft state persistence.
 * @param draftDirectory File layout for draft storage.
 */
public class ResumeDraftCoordinator(
    private val draftStateDao: DraftStateDao,
    private val draftDirectory: DraftDirectory,
) {

    /**
     * Returns all draft state entries that may be resumable.
     *
     * The caller should check each returned entity's [DraftStateEntity.lastStateName]
     * and [DraftStateEntity.pendingActions] to determine the correct rehydration
     * path. The caller is responsible for filtering by recency if desired.
     */
    public suspend fun loadResumableDrafts(): List<DraftStateEntity> {
        return draftStateDao.listAll().first()
    }

    /**
     * Attempts to resume the draft identified by [draftId].
     *
     * 1. Checks that the draft directory and its manifest file exist on disk.
     * 2. If they exist, returns the [DraftId] for rehydration.
     * 3. If they do not exist, deletes the database row and returns null.
     *
     * @param draftId The draft to resume.
     * @return The [DraftId] if resumable, null if the files were cleaned up.
     */
    public suspend fun resumeDraft(draftId: String): DraftId? {
        val id = DraftId(draftId)
        val draftPath = draftDirectory.draftPath(id)
        val manifestPath = draftDirectory.manifestPath(id)

        return if (Files.exists(draftPath) && Files.exists(manifestPath)) {
            id
        } else {
            draftStateDao.delete(draftId)
            null
        }
    }
}
