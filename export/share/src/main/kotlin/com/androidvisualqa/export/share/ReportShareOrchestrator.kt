package com.androidvisualqa.export.share

import android.content.Intent
import android.net.Uri
import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.report.ZipExporter
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * Top-level orchestrator that wires [FileProviderWriter], [MediaStoreWriter],
 * and [ZipExporter] to deliver share and save-to-Downloads flows.
 *
 * ## Share ZIP flow
 *
 * 1. Calls [ZipExporter.export] to produce a ZIP in a temp directory.
 * 2. Reads the ZIP into memory.
 * 3. Writes it via [FileProviderWriter.writeBytesToCache] to get a `content://` URI.
 * 4. Builds a share [Intent] via [ShareIntentBuilder.buildShareIntent].
 *
 * ## Save to Downloads flow
 *
 * 1. Same ZIP production step.
 * 2. Delegates to [MediaStoreWriter.saveZipToDownloads].
 *
 * @param fileProviderWriter   Writer for FileProvider-visible cache locations.
 * @param mediaStoreWriter     Writer for the public Downloads directory.
 * @param zipExporter          The [ZipExporter] from `:report` that produces the ZIP bundle.
 */
public class ReportShareOrchestrator(
    private val fileProviderWriter: FileProviderWriter,
    private val mediaStoreWriter: MediaStoreWriter,
    private val zipExporter: ZipExporter,
) {

    /**
     * Produces a ZIP and returns a shareable URI + intent pair.
     *
     * @param report        The report to export.
     * @param originalPng   Raw bytes of the original screenshot PNG.
     * @param annotatedPng  Raw bytes of the annotated screenshot PNG.
     * @return [Result.success] with the FileProvider [Uri] and share [Intent],
     *         or [Result.failure].
     */
    public suspend fun shareAsZip(
        report: VisualFeedbackReport,
        originalPng: ByteArray,
        annotatedPng: ByteArray,
    ): Result<Pair<Uri, Intent>> = runCatching {
        val (uri, _) = produceZipAndUri(report, originalPng, annotatedPng)
        val intent = ShareIntentBuilder.buildShareIntent(uri)
        uri to intent
    }

    /**
     * Produces a ZIP and saves it to the public Downloads directory.
     *
     * @param report        The report to export.
     * @param originalPng   Raw bytes of the original screenshot PNG.
     * @param annotatedPng  Raw bytes of the annotated screenshot PNG.
     * @return [Result.success] with the content [Uri] from MediaStore,
     *         or [Result.failure].
     */
    public suspend fun saveZipToDownloads(
        report: VisualFeedbackReport,
        originalPng: ByteArray,
        annotatedPng: ByteArray,
    ): Result<Uri> = runCatching {
        val tempDir = createTempDirForReport(report)
        try {
            val originalPath = writeToTemp(tempDir, "original.png", originalPng)
            val annotatedPath = writeToTemp(tempDir, "annotated.png", annotatedPng)
            val zipPath = tempDir.resolve("${report.reportId.value}.zip")

            zipExporter.export(report, originalPath, annotatedPath, zipPath).getOrThrow()

            val zipBytes = Files.readAllBytes(zipPath)
            val filename = "${report.reportId.value}.zip"
            mediaStoreWriter.saveZipToDownloads(zipBytes, filename).getOrThrow()
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    // --- Internal helpers ---

    private suspend fun produceZipAndUri(
        report: VisualFeedbackReport,
        originalPng: ByteArray,
        annotatedPng: ByteArray,
    ): Pair<Uri, ByteArray> {
        val tempDir = createTempDirForReport(report)
        try {
            val originalPath = writeToTemp(tempDir, "original.png", originalPng)
            val annotatedPath = writeToTemp(tempDir, "annotated.png", annotatedPng)
            val zipPath = tempDir.resolve("${report.reportId.value}.zip")

            zipExporter.export(report, originalPath, annotatedPath, zipPath).getOrThrow()

            val zipBytes = Files.readAllBytes(zipPath)
            val uri = fileProviderWriter.writeBytesToCache(
                subdir = "exports",
                filename = "${report.reportId.value}.zip",
                bytes = zipBytes,
            ).getOrThrow()

            return uri to zipBytes
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    private fun createTempDirForReport(report: VisualFeedbackReport): Path {
        val dir = File(System.getProperty("java.io.tmpdir"), "vqa-export-${report.reportId.value}")
        dir.mkdirs()
        return dir.toPath()
    }

    private fun writeToTemp(dir: Path, name: String, bytes: ByteArray): Path {
        val file = dir.resolve(name)
        Files.write(file, bytes)
        return file
    }

    private fun File.deleteRecursively() {
        if (isDirectory) {
            listFiles()?.forEach { it.deleteRecursively() }
        }
        delete()
    }
}
