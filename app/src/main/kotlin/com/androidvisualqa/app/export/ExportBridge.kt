package com.androidvisualqa.app.export

import android.content.Context
import android.net.Uri
import com.androidvisualqa.export.share.FileProviderWriter
import com.androidvisualqa.export.share.MediaStoreWriter
import com.androidvisualqa.export.share.ReportShareOrchestrator
import com.androidvisualqa.files.DraftDirectory
import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.report.ZipExporter
import java.nio.file.Files

/**
 * Orchestrates the export of a [VisualFeedbackReport] for the share sheet
 * and "Save to Downloads" flows.
 *
 * ## Share ZIP flow
 *
 * Delegates to [ReportShareOrchestrator.shareAsZip] to produce the ZIP and
 * share intent. This module handles extracting PNG bytes from the draft
 * directory so the orchestrator stays pure.
 *
 * ## Save to Downloads flow
 *
 * Delegates to [ReportShareOrchestrator.saveZipToDownloads].
 *
 * @param context Application [Context] for file resolution.
 * @param draftDirectory [DraftDirectory] to locate original/annotated PNGs.
 */
public class ExportBridge(
    private val context: Context,
    private val draftDirectory: DraftDirectory,
) {
    private val orchestrator = ReportShareOrchestrator(
        fileProviderWriter = FileProviderWriter(context),
        mediaStoreWriter = MediaStoreWriter(context),
        zipExporter = ZipExporter(),
    )

    /**
     * Produces a ZIP bundle and returns a FileProvider URI suitable for a share sheet.
     *
     * @param report The report to export.
     * @return [Result.success] with the FileProvider [Uri].
     */
    public suspend fun exportAndShare(report: VisualFeedbackReport): Result<Uri> {
        val pngs = readPngBytes(report)
        return orchestrator.shareAsZip(report, pngs.first, pngs.second)
            .map { (uri, _) -> uri }
    }

    /**
     * Saves the report ZIP to the public Downloads directory.
     *
     * @param report The report to export.
     * @return [Result.success] with the content [Uri] of the saved file.
     */
    public suspend fun saveToDownloads(report: VisualFeedbackReport): Result<Uri> {
        val pngs = readPngBytes(report)
        return orchestrator.saveZipToDownloads(report, pngs.first, pngs.second)
    }

    // --- Internal helpers ---

    private fun readPngBytes(report: VisualFeedbackReport): Pair<ByteArray, ByteArray> {
        val draftId = DraftId(report.capture.sessionId)
        val draftPath = draftDirectory.draftPath(draftId)

        val originalPng = draftPath.resolve("original.png")
        val annotatedPng = draftPath.resolve("annotated.png")

        val effectiveOriginal = if (Files.exists(originalPng)) originalPng
        else draftDirectory.originalImagePath(draftId)

        val effectiveAnnotated = if (Files.exists(annotatedPng)) annotatedPng
        else draftDirectory.annotatedImagePath(draftId)

        return Files.readAllBytes(effectiveOriginal) to Files.readAllBytes(effectiveAnnotated)
    }
}
