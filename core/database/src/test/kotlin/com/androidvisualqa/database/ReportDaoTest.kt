package com.androidvisualqa.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportDaoTest {

    private lateinit var db: ReportDatabase
    private lateinit var dao: ReportDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ReportDatabase::class.java).build()
        dao = db.reports()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertAndGet() = runBlocking {
        val entity = reportEntity("r1")
        dao.upsert(entity)

        val loaded = dao.get("r1")
        assertNotNull(loaded)
        assertEquals("r1", loaded?.reportId)
        assertEquals("com.example.app", loaded?.packageName)
    }

    @Test
    fun getMissingReturnsNull() = runBlocking {
        val loaded = dao.get("nonexistent")
        assertNull(loaded)
    }

    @Test
    fun listRecentReturnsNewestFirst() = runBlocking {
        dao.upsert(reportEntity("r1", createdAt = 1000))
        dao.upsert(reportEntity("r2", createdAt = 2000))
        dao.upsert(reportEntity("r3", createdAt = 3000))

        val list = dao.listRecent(10).first()

        assertEquals(3, list.size)
        assertEquals("r3", list[0].reportId)
        assertEquals("r2", list[1].reportId)
        assertEquals("r1", list[2].reportId)
    }

    @Test
    fun listRecentRespectsLimit() = runBlocking {
        dao.upsert(reportEntity("r1", createdAt = 1000))
        dao.upsert(reportEntity("r2", createdAt = 2000))
        dao.upsert(reportEntity("r3", createdAt = 3000))

        val list = dao.listRecent(2).first()
        assertEquals(2, list.size)
    }

    @Test
    fun countReturnsCorrectNumber() = runBlocking {
        assertEquals(0, dao.count())

        dao.upsert(reportEntity("r1", createdAt = 1000))
        assertEquals(1, dao.count())

        dao.upsert(reportEntity("r2", createdAt = 2000))
        assertEquals(2, dao.count())
    }

    @Test
    fun deleteOlderThanRemovesOldReports() = runBlocking {
        dao.upsert(reportEntity("r1", createdAt = 1000))
        dao.upsert(reportEntity("r2", createdAt = 2000))
        dao.upsert(reportEntity("r3", createdAt = 3000))

        val deleted = dao.deleteOlderThan(2500)
        assertEquals(2, deleted)
        assertEquals(1, dao.count())
        assertNull(dao.get("r1"))
        assertNull(dao.get("r2"))
        assertNotNull(dao.get("r3"))
    }

    @Test
    fun upsertReplacesExisting() = runBlocking {
        dao.upsert(reportEntity("r1", packageName = "original"))
        dao.upsert(reportEntity("r1", packageName = "updated"))

        val loaded = dao.get("r1")
        assertEquals("updated", loaded?.packageName)
        assertEquals(1, dao.count())
    }

    private fun reportEntity(
        id: String = "test-id",
        createdAt: Long = 1000,
        packageName: String = "com.example.app",
    ) = ReportEntity(
        reportId = id,
        createdAt = createdAt,
        status = "Draft",
        packageName = packageName,
        lastStateName = "Idle",
        schemaVersion = 1,
        reportJsonPath = "drafts/$id/report.json",
        annotatedPngPath = "drafts/$id/annotated.png",
        sha256 = "abc123",
    )
}
