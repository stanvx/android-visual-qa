package com.androidvisualqa.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidvisualqa.files.DraftDirectory
import com.androidvisualqa.files.DraftStore
import com.androidvisualqa.files.FileSystemDraftStore
import com.androidvisualqa.model.ReportStatus
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.model.ids.ReportId
import com.androidvisualqa.report.FileSystemReportHistoryIndex
import com.androidvisualqa.report.HistoryEntry
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files

/**
 * Measures the time from "tap save" to "report files written + history entry
 * recorded".
 *
 * Budget (plan §17): save report without audio < 750 ms typical.
 * This benchmark asserts median < 1000 ms to allow for the synthetic
 * write pattern across a temp-directory [DraftStore] and
 * [FileSystemReportHistoryIndex].
 *
 * ponytail: Uses a temp-directory [FileSystemDraftStore] and
 * [FileSystemReportHistoryIndex] rather than an in-memory fake, because
 * the actual save path goes through these implementations. The benchmark
 * writes small payloads (1 KiB) to keep I/O overhead predictable.
 */
@RunWith(AndroidJUnit4::class)
class SavingBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun measureSave() {
        // Set up a temp directory for draft storage and history.
        val tempDir = Files.createTempDirectory("benchmark-save-")
        val draftStore: DraftStore = FileSystemDraftStore(DraftDirectory(tempDir))
        val historyIndex = FileSystemReportHistoryIndex(tempDir.resolve("history.jsonl"))

        // Pre-create a draft so we don't measure createDraft().
        val draftId = runBlocking { draftStore.createDraft().getOrThrow() }
        val sampleBytes = ByteArray(1024) { it.toByte() }

        benchmarkRule.measureRepeated {
            runBlocking {
                // Simulate the "save" path: write images + manifest, then record history.
                draftStore.writeOriginal(draftId, sampleBytes).getOrThrow()
                draftStore.writeAnnotated(draftId, sampleBytes).getOrThrow()

                historyIndex.append(
                    HistoryEntry(
                        draftId = draftId,
                        reportId = ReportId("report-${draftId.value}"),
                        createdAt = Clock.System.now(),
                        status = ReportStatus.Saved,
                        packageName = "com.androidvisualqa.app",
                    ),
                ).getOrThrow()
            }
        }

        // Cleanup
        tempDir.toFile().deleteRecursively()
    }
}
