package com.androidvisualqa.report

import com.androidvisualqa.model.ReportStatus
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.model.ids.ReportId
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ReportHistoryIndexTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `list returns empty for non-existent index`() {
        val indexPath = tempDir.resolve(".history.jsonl")
        val index = FileSystemReportHistoryIndex(indexPath)
        val entries = kotlinx.coroutines.runBlocking { index.list() }
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `append stores entries in order`() {
        val indexPath = tempDir.resolve(".history.jsonl")
        val index = FileSystemReportHistoryIndex(indexPath)
        val now = Instant.parse("2026-07-28T12:00:00Z")

        val e1 = HistoryEntry(
            DraftId("draft-1"), ReportId("report-1"), now, ReportStatus.Saved, "com.example.app1",
        )
        val e2 = HistoryEntry(
            DraftId("draft-2"), ReportId("report-2"), now, ReportStatus.Exported, "com.example.app2",
        )
        val e3 = HistoryEntry(
            DraftId("draft-3"), ReportId("report-3"), now, ReportStatus.Failed, "com.example.app3",
        )

        kotlinx.coroutines.runBlocking {
            assertTrue(index.append(e1).isSuccess)
            assertTrue(index.append(e2).isSuccess)
            assertTrue(index.append(e3).isSuccess)
        }

        val entries = kotlinx.coroutines.runBlocking { index.list() }
        assertEquals(3, entries.size)
        assertEquals(DraftId("draft-1"), entries[0].draftId)
        assertEquals(ReportId("report-1"), entries[0].reportId)
        assertEquals(DraftId("draft-2"), entries[1].draftId)
        assertEquals(ReportId("report-2"), entries[1].reportId)
        assertEquals(DraftId("draft-3"), entries[2].draftId)
        assertEquals(ReportId("report-3"), entries[2].reportId)
        assertEquals("com.example.app1", entries[0].packageName)
        assertEquals("com.example.app2", entries[1].packageName)
        assertEquals("com.example.app3", entries[2].packageName)
    }

    @Test
    fun `jsonl is line-delimited`() {
        val indexPath = tempDir.resolve(".history.jsonl")
        val index = FileSystemReportHistoryIndex(indexPath)
        val now = Instant.parse("2026-07-28T12:00:00Z")

        kotlinx.coroutines.runBlocking {
            index.append(HistoryEntry(DraftId("d-1"), ReportId("r-1"), now, ReportStatus.Saved, "com.example"))
            index.append(HistoryEntry(DraftId("d-2"), ReportId("r-2"), now, ReportStatus.Saved, "com.example"))
        }

        val lines = Files.readAllLines(indexPath)
        assertEquals(2, lines.size)
        assertTrue(lines[0].isNotBlank())
        assertTrue(lines[1].isNotBlank())
        assertTrue(lines[0].contains("d-1"))
        assertTrue(lines[1].contains("d-2"))
    }

}
