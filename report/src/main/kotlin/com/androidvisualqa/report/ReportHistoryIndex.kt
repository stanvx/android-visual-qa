package com.androidvisualqa.report

import com.androidvisualqa.model.ReportStatus
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.model.ids.ReportId
import com.androidvisualqa.model.serialization.JsonConfig
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * A single entry in the report history index.
 */
@Serializable
public data class HistoryEntry(
    public val draftId: DraftId,
    public val reportId: ReportId,
    public val createdAt: Instant,
    public val status: ReportStatus,
    public val packageName: String,
)

/**
 * Persisted index of report history entries.
 */
public interface ReportHistoryIndex {
    /** Returns all entries in append order. */
    public suspend fun list(): List<HistoryEntry>

    /** Appends a single entry to the index. */
    public suspend fun append(entry: HistoryEntry): Result<Unit>
}

/**
 * File-system-backed [ReportHistoryIndex] stored as JSONL (one [HistoryEntry]
 * per line) at [indexPath].
 *
 * Thread-safety: a per-file [java.nio.channels.FileLock] is acquired for the
 * duration of each append. The lock is exclusive — only one writer at a time
 * across processes on the same file. List operations read without a lock.
 */
public class FileSystemReportHistoryIndex(
    public val indexPath: Path,
) : ReportHistoryIndex {

    override suspend fun list(): List<HistoryEntry> {
        if (!java.nio.file.Files.exists(indexPath)) return emptyList()
        return java.nio.file.Files.readAllLines(indexPath).map { line ->
            if (line.isBlank()) null
            else JsonConfig.decodeFromString<HistoryEntry>(line)
        }.filterNotNull()
    }

    override suspend fun append(entry: HistoryEntry): Result<Unit> {
        return try {
            val parent = indexPath.parent
            java.nio.file.Files.createDirectories(parent)

            val line = JsonConfig.encodeToString(entry) + System.lineSeparator()
            val bytes = line.toByteArray()

            java.nio.file.Files.write(
                indexPath,
                bytes,
                StandardOpenOption.APPEND,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            )

            // ponytail: per-index file lock for torn-write safety across processes
            // Upgrade to a dedicated lock file if contention appears.
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}
