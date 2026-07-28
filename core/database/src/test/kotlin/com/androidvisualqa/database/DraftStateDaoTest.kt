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
class DraftStateDaoTest {

    private lateinit var db: ReportDatabase
    private lateinit var dao: DraftStateDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ReportDatabase::class.java).build()
        dao = db.draftStateDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertAndGet() = runBlocking {
        val entity = DraftStateEntity(
            draftId = "draft-1",
            lastStateName = "CapturingPixels",
            updatedAt = 1000,
            pendingActions = "[]",
        )
        dao.upsert(entity)

        val loaded = dao.get("draft-1")
        assertNotNull(loaded)
        assertEquals("CapturingPixels", loaded?.lastStateName)
    }

    @Test
    fun getMissingReturnsNull() = runBlocking {
        val loaded = dao.get("nonexistent")
        assertNull(loaded)
    }

    @Test
    fun deleteRemovesEntity() = runBlocking {
        val entity = DraftStateEntity(
            draftId = "draft-1",
            lastStateName = "Idle",
            updatedAt = 1000,
            pendingActions = "[]",
        )
        dao.upsert(entity)
        assertEquals("Idle", dao.get("draft-1")?.lastStateName)

        dao.delete("draft-1")
        assertNull(dao.get("draft-1"))
    }

    @Test
    fun upsertReplacesExisting() = runBlocking {
        dao.upsert(DraftStateEntity("draft-1", "Idle", 1000, "[]"))
        dao.upsert(DraftStateEntity("draft-1", "Annotating", 2000, "[]"))

        val loaded = dao.get("draft-1")
        assertEquals("Annotating", loaded?.lastStateName)
    }

    @Test
    fun listAllReturnsAllNewestFirst() = runBlocking {
        dao.upsert(DraftStateEntity("draft-1", "Idle", 1000, "[]"))
        dao.upsert(DraftStateEntity("draft-2", "Capturing", 2000, "[]"))
        dao.upsert(DraftStateEntity("draft-3", "Complete", 3000, "[]"))

        val list = dao.listAll().first()

        assertEquals(3, list.size)
        assertEquals("draft-3", list[0].draftId)
        assertEquals("draft-2", list[1].draftId)
        assertEquals("draft-1", list[2].draftId)
    }
}
