package com.androidvisualqa.app.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.androidvisualqa.files.DraftDirectory
import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.report.ZipExporter
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Orchestrates the export of a [VisualFeedbackReport] for the share sheet
 * and "Save to Downloads" flows.
 *
 * ## Share ZIP flow
 *
 * 1. Calls [ZipExporter] to produce a ZIP bundle in `cacheDir/exports/{reportId}.zip`.
 * 2. Returns a `content://` URI via [FileProvider] for [Intent.ACTION_SEND].
 *
 * ## Save to Downloads flow
 *
 * 1. On API 29+: writes via [MediaStore.Downloads] (no file permission needed).
 * 2. On API < 29: writes to app cache then copies via [FileProvider].
 *
 * @param context Application [Context] for file resolution.
 * @param draftDirectory [DraftDirectory] to locate original/animated PNGs.
 */
public class ExportBridge(
    private val context: Context,
    private val draftDirectory: DraftDirectory,
) {
    private val zipExporter = ZipExporter()
    private val cacheExportsDir: File = File(context.cacheDir, "exports")

    /**
     * Produces a ZIP bundle and returns a FileProvider URI suitable for a share sheet.
     *
     * @param report The report to export.
     * @return [Result.success] with the FileProvider [android.net.Uri].
     */
    public suspend fun exportAndShare(report: VisualFeedbackReport): Result<android.net.Uri> {
        return runCatching {
            val exportDir = cacheExportsDir.also { it.mkdirs() }
            val zipPath = exportDir.toPath().resolve("${report.reportId.value}.zip")
            val draftId = report.capture.sessionId
            val draftPath = draftDirectory.draftPath(com.androidvisualqa.model.ids.DraftId(draftId))

            val originalPng = draftPath.resolve("original.png")
            val annotatedPng = draftPath.resolve("annotated.png")

            // If files don't exist at draft path, try at files/reports/draftId
            val effectiveOriginal = if (Files.exists(originalPng)) originalPng else draftDirectory.originalImagePath(com.androidvisualqa.model.ids.DraftId(draftId))
            val effectiveAnnotated = if (Files.exists(annotatedPng)) annotatedPng else draftDirectory.annotatedImagePath(com.androidvisualqa.model.ids.DraftId(draftId))

            zipExporter.export(report, effectiveOriginal, effectiveAnnotated, zipPath).getOrThrow()

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipPath.toFile(),
            )
        }
    }

    /**
     * Saves the report ZIP to the public Downloads directory.
     *
     * On API 29+: uses [MediaStore.Downloads].
     * On API < 29: copies via FileProvider to a temp location the user can access.
     *
     * @param report The report to export.
     * @return [Result.success] with the [android.net.Uri] of the saved file.
     */
    public suspend fun saveToDownloads(report: VisualFeedbackReport): Result<android.net.Uri> {
        return runCatching {
            val exportDir = cacheExportsDir.also { it.mkdirs() }
            val zipSource = exportDir.toPath().resolve("${report.reportId.value}.zip")
            val draftId = report.capture.sessionId
            val draftPath = draftDirectory.draftPath(com.androidvisualqa.model.ids.DraftId(draftId))

            val originalPng = draftPath.resolve("original.png")
            val annotatedPng = draftPath.resolve("annotated.png")

            val effectiveOriginal = if (Files.exists(originalPng)) originalPng else draftDirectory.originalImagePath(com.androidvisualqa.model.ids.DraftId(draftId))
            val effectiveAnnotated = if (Files.exists(annotatedPng)) annotatedPng else draftDirectory.annotatedImagePath(com.androidvisualqa.model.ids.DraftId(draftId))

            // Ensure ZIP exists
            if (!Files.exists(zipSource)) {
                zipExporter.export(report, effectiveOriginal, effectiveAnnotated, zipSource).getOrThrow()
            }

            if (Build.VERSION.SDK_INT >= 29) {
                // MediaStore API
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "${report.reportId.value}.zip")
                    put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException("Failed to create MediaStore entry")

                resolver.openOutputStream(uri)?.use { output ->
                    Files.newInputStream(zipSource).use { input ->
                        input.transferTo(output)
                    }
                } ?: throw IllegalStateException("Failed to open output stream for Downloads")

                uri
            } else {
                // Pre-API 29: use FileProvider to share from cache
                Files.copy(
                    zipSource,
                    cacheExportsDir.toPath().resolve("download-${report.reportId.value}.zip"),
                    StandardCopyOption.REPLACE_EXISTING,
                )
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    cacheExportsDir.resolve("download-${report.reportId.value}.zip"),
                )
            }
        }
    }
}
