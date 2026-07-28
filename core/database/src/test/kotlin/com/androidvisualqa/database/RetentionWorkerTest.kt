package com.androidvisualqa.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidvisualqa.files.DraftStore
import com.androidvisualqa.model.ids.DraftId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RetentionWorkerTest {

    private lateinit var db: ReportDatabase
    private lateinit var dao: ReportDao
    private lateinit var draftStore: FakeDraftStore

    companion object {
        // Use timestamps relative to now to avoid the age-based deletion trigger
        private val NOW = System.currentTimeMillis()
        private val DAY = 86_400_000L
        private val T1 = NOW - (4 * DAY)  // 4 days ago
        private val T2 = NOW - (3 * DAY)  // 3 days ago
        private val T3 = NOW - (2 * DAY)  // 2 days ago
        private val T4 = NOW - (1 * DAY)  // 1 day ago
        private val T5 = NOW              // now
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, ReportDatabase::class.java).build()
        dao = db.reports()
        draftStore = FakeDraftStore()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun verifyDeleteByIdWorks() = runBlocking {
        dao.upsert(reportEntity("r1", createdAt = T1))
        dao.upsert(reportEntity("r2", createdAt = T2))
        assertEquals(2, dao.count())

        dao.deleteById("r1")
        assertEquals(1, dao.count())
        assertNull(dao.get("r1"))
        assertEquals("r2", dao.get("r2")?.reportId)
    }

    @Test
    fun workerDeletesOldestWhenExceedingMaxDrafts() = runBlocking {
        dao.upsert(reportEntity("r1", createdAt = T1, draftId = "draft-1"))
        dao.upsert(reportEntity("r2", createdAt = T2, draftId = "draft-2"))
        dao.upsert(reportEntity("r3", createdAt = T3, draftId = "draft-3"))
        dao.upsert(reportEntity("r4", createdAt = T4, draftId = "draft-4"))
        dao.upsert(reportEntity("r5", createdAt = T5, draftId = "draft-5"))
        assertEquals(5, dao.count())

        val policy = RetentionPolicy(maxDrafts = 3, maxDays = 365, minFreeBytes = 0)
        val result = deleteExcessReports(dao, draftStore, policy, freeBytes = Long.MAX_VALUE)
        assertTrue(result)

        // 2 oldest (r1, r2) removed, 3 newest (r3, r4, r5) remain
        assertEquals(3, dao.count())
        assertTrue(draftStore.deletedDraftIds.contains("draft-1"))
        assertTrue(draftStore.deletedDraftIds.contains("draft-2"))
        assertEquals(2, draftStore.deletedDraftIds.size)

        assertEquals("r3", dao.get("r3")?.reportId)
        assertEquals("r4", dao.get("r4")?.reportId)
        assertEquals("r5", dao.get("r5")?.reportId)
    }

    @Test
    fun workerDoesNothingWhenUnderLimit() = runBlocking {
        dao.upsert(reportEntity("r1", createdAt = T1, draftId = "draft-1"))
        dao.upsert(reportEntity("r2", createdAt = T2, draftId = "draft-2"))
        assertEquals(2, dao.count())

        val policy = RetentionPolicy(maxDrafts = 10, maxDays = 365, minFreeBytes = 0)
        val result = deleteExcessReports(dao, draftStore, policy, freeBytes = Long.MAX_VALUE)
        assertTrue(result)

        assertEquals(2, dao.count())
        assertTrue(draftStore.deletedDraftIds.isEmpty())
    }

    @Test
    fun workerHandlesEmptyDatabase() = runBlocking {
        val result = deleteExcessReports(dao, draftStore, RetentionPolicy(), freeBytes = Long.MAX_VALUE)
        assertTrue(result)
        assertTrue(draftStore.deletedDraftIds.isEmpty())
    }

    private suspend fun deleteExcessReports(
        dao: ReportDao,
        store: DraftStore,
        retentionPolicy: RetentionPolicy,
        freeBytes: Long,
    ): Boolean {
        var totalCount = dao.count()
        if (totalCount == 0) return true

        while (true) {
            val allReports = dao.listAll()
            if (allReports.isEmpty()) break

            val oldestCreatedAt = allReports.minOf { it.createdAt }

            val toDelete = allReports.filter { candidate ->
                retentionPolicy.shouldDelete(candidate, totalCount, oldestCreatedAt, freeBytes)
            }

            if (toDelete.isEmpty()) break

            for (report in toDelete) {
                report.draftId?.let { draftId ->
                    store.deleteDraft(DraftId(draftId))
                }
                dao.deleteById(report.reportId)
            }

            totalCount = dao.count()
        }

        return true
    }

    private fun reportEntity(
        id: String,
        createdAt: Long,
        draftId: String? = null,
    ) = ReportEntity(
        reportId = id,
        draftId = draftId,
        createdAt = createdAt,
        status = "Saved",
        packageName = "com.example.app",
        lastStateName = "Complete",
        schemaVersion = 1,
        reportJsonPath = "drafts/$id/report.json",
        annotatedPngPath = "drafts/$id/annotated.png",
        sha256 = "sha256-of-$id",
    )
}

internal class FakeDraftStore : DraftStore {
    val deletedDraftIds = mutableListOf<String>()

    override suspend fun createDraft(): Result<DraftId> =
        Result.failure(UnsupportedOperationException("not needed"))

    override suspend fun writeOriginal(id: DraftId, bytes: ByteArray): Result<java.nio.file.Path> =
        Result.failure(UnsupportedOperationException("not needed"))

    override suspend fun writeAnnotated(id: DraftId, bytes: ByteArray): Result<java.nio.file.Path> =
        Result.failure(UnsupportedOperationException("not needed"))

    override suspend fun writeManifest(id: DraftId, manifest: com.androidvisualqa.files.DraftManifest): Result<java.nio.file.Path> =
        Result.failure(UnsupportedOperationException("not needed"))

    override suspend fun readDraft(id: DraftId): Result<com.androidvisualqa.files.DraftManifest?> =
        Result.success(null)

    override suspend fun deleteDraft(id: DraftId): Result<Unit> {
        deletedDraftIds.add(id.value)
        return Result.success(Unit)
    }
}
