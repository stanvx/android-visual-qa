package com.androidvisualqa.files

import com.androidvisualqa.model.ids.DraftId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class FileSystemDraftStoreTest {

    private val now = Instant.parse("2026-07-28T10:00:00Z")

    @Test
    fun `full round-trip create write read delete`(@TempDir tempDir: Path) = runTest {
        val store = FileSystemDraftStore(DraftDirectory(tempDir))

        // Create
        val createResult = store.createDraft()
        assertTrue(createResult.isSuccess)
        val draftId = createResult.getOrThrow()
        assertTrue(Files.exists(tempDir.resolve("drafts").resolve(draftId.value)))

        // Write original
        val originalBytes = "screenshot-data".toByteArray()
        val origResult = store.writeOriginal(draftId, originalBytes)
        assertTrue(origResult.isSuccess)

        // Write manifest
        val manifest = DraftManifest(
            draftId = draftId,
            createdAt = now,
            originalSha256 = Hashing.sha256(originalBytes),
            reportSchemaVersion = 1,
            captureState = "Complete",
        )
        val manResult = store.writeManifest(draftId, manifest)
        assertTrue(manResult.isSuccess)

        // Read back
        val readResult = store.readDraft(draftId)
        assertTrue(readResult.isSuccess)
        val restored = readResult.getOrThrow()
        assertNotNull(restored)
        assertEquals(draftId, restored!!.draftId)
        assertEquals("Complete", restored.captureState)
        assertEquals(manifest.originalSha256, restored.originalSha256)

        // Delete
        val deleteResult = store.deleteDraft(draftId)
        assertTrue(deleteResult.isSuccess)
        assertTrue(Files.notExists(tempDir.resolve("drafts").resolve(draftId.value)))
    }

    @Test
    fun `readDraft returns null for non-existent draft`(@TempDir tempDir: Path) = runTest {
        val store = FileSystemDraftStore(DraftDirectory(tempDir))
        val result = store.readDraft(DraftId("non-existent"))
        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    @Test
    fun `concurrent writes to different ids do not interfere`(@TempDir tempDir: Path) = runTest {
        val store = FileSystemDraftStore(DraftDirectory(tempDir))

        val id1Result = store.createDraft()
        val id2Result = store.createDraft()
        assertTrue(id1Result.isSuccess)
        assertTrue(id2Result.isSuccess)
        val id1 = id1Result.getOrThrow()
        val id2 = id2Result.getOrThrow()

        // Write to both
        val m1 = DraftManifest(id1, now, reportSchemaVersion = 1, captureState = "Draft")
        val m2 = DraftManifest(id2, now, reportSchemaVersion = 1, captureState = "Complete")
        store.writeManifest(id1, m1)
        store.writeManifest(id2, m2)

        val r1 = store.readDraft(id1).getOrThrow()!!
        val r2 = store.readDraft(id2).getOrThrow()!!

        assertEquals("Draft", r1.captureState)
        assertEquals("Complete", r2.captureState)
        assertTrue(r1.draftId != r2.draftId)
    }
}
