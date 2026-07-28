package com.androidvisualqa.database

import android.content.Context
import android.os.StatFs
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.androidvisualqa.files.DraftStore
import com.androidvisualqa.files.FileSystemDraftStore
import com.androidvisualqa.files.DraftDirectory
import com.androidvisualqa.model.ids.DraftId
import java.io.File

/**
 * WorkManager worker that enforces the report retention policy.
 *
 * Cascade order:
 * 1. Load all reports from the database.
 * 2. Delete reports until all [RetentionPolicy] criteria are satisfied.
 * 3. Delete the corresponding draft directories from [DraftStore].
 *
 * Schedule via [androidx.work.WorkManager.enqueueUniquePeriodicWork] once per day.
 * Scheduling belongs in `:app`, not here.
 */
class RetentionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    /** Visible for testing — override to inject fakes. */
    internal var database: ReportDatabase = ReportDatabase.build(context)
    internal var draftStore: DraftStore = FileSystemDraftStore(
        DraftDirectory(File(context.filesDir, "reports").toPath())
    )
    internal var policy: RetentionPolicy = RetentionPolicy()

    /** Testing hook — set to override [freeDiskBytes] return value. */
    internal var freeBytesOverride: Long? = null

    override suspend fun doWork(): Result {
        return try {
            val dao = database.reports()
            deleteExcessReports(dao, draftStore, policy)
        } catch (e: Exception) {
            if (isRetryable(e)) Result.retry() else Result.failure()
        }
    }

    /**
     * Deletes reports until the retention policy is satisfied.
     * Exposed as internal for direct testing without WorkManager lifecycle.
     *
     * Low-disk guard: if free disk space falls below [policy.minFreeBytes],
     * the worker deletes the oldest report one at a time, rechecking free
     * space after each deletion, until space is recovered or the store is empty.
     */
    internal suspend fun deleteExcessReports(
        dao: ReportDao,
        store: DraftStore,
        retentionPolicy: RetentionPolicy,
        freeBytes: Long = freeDiskBytes(applicationContext.filesDir.absolutePath),
    ): Result {
        var totalCount = dao.count()
        if (totalCount == 0) return Result.success()

        val minFreeBytes = 500L * 1024 * 1024 // 500 MB

        while (true) {
            // Low-disk guard: delete oldest one-at-a-time until space is recovered
            if (freeBytes < minFreeBytes) {
                val allReports = dao.listAll().sortedBy { it.createdAt }
                val oldest = allReports.firstOrNull() ?: return Result.success()
                if (oldest.draftId != null) store.deleteDraft(DraftId(oldest.draftId))
                dao.deleteById(oldest.reportId)
                totalCount = dao.count()
                val newFree = freeDiskBytes(applicationContext.filesDir.absolutePath)
                if (newFree >= minFreeBytes || totalCount == 0) return Result.success()
                continue
            }

            val allReports = dao.listAll()
            if (allReports.isEmpty()) break

            val oldestCreatedAt = allReports.minOf { it.createdAt }

            val toDelete = allReports.filter { candidate ->
                retentionPolicy.shouldDelete(candidate, totalCount, oldestCreatedAt)
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

        return Result.success()
    }

    private fun isRetryable(e: Exception): Boolean {
        val message = e.message?.lowercase() ?: ""
        return message.contains("temp") ||
            message.contains("disk") ||
            message.contains("timeout") ||
            message.contains("eagain") ||
            message.contains("ebusy")
    }

    private fun freeDiskBytes(path: String): Long {
        return try {
            val stat = StatFs(path)
            stat.availableBytes
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
    }
}
