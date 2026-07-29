package com.androidvisualqa.app.ui.history

import com.androidvisualqa.files.DraftDirectory
import com.androidvisualqa.files.FileSystemDraftStore
import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.model.serialization.JsonConfig
import com.androidvisualqa.report.FileSystemReportHistoryIndex
import com.androidvisualqa.report.HistoryEntry
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import java.nio.file.Files
import java.nio.file.Path

public data class CaptureHistoryItem(
    val draftId: DraftId,
    val packageName: String,
    val createdAt: Instant,
    val status: String,
    val commentCount: Int,
    val thumbnailPath: Path?,
    val isDraft: Boolean,
)

/** Reads the existing report index and draft directories into dashboard cards. */
public class CaptureHistoryRepository(
    private val draftStore: FileSystemDraftStore,
    private val reportHistory: FileSystemReportHistoryIndex,
) {
    public suspend fun load(): List<CaptureHistoryItem> {
        val entries = reportHistory.list()
            .groupBy(HistoryEntry::draftId)
            .mapValues { (_, values) -> values.maxBy(HistoryEntry::createdAt) }
        val manifests = listManifests(draftStore.directory)
        val ids = entries.keys + manifests.keys

        return ids.mapNotNull { id ->
            val manifest = manifests[id]
            val entry = entries[id]
            val createdAt = entry?.createdAt ?: manifest?.createdAt ?: return@mapNotNull null
            val packageName = entry?.packageName ?: manifest?.packageName ?: "unknown"
            val reportPath = draftStore.directory.reportJsonPath(id)
            val commentCount = readCommentCount(
                reportPath,
                draftStore.directory.editorStatePath(id),
            )

            CaptureHistoryItem(
                draftId = id,
                packageName = packageName,
                createdAt = createdAt,
                status = entry?.status?.name ?: manifest?.captureState ?: "Draft",
                commentCount = commentCount,
                thumbnailPath = thumbnailPath(draftStore.directory, id),
                isDraft = entry == null,
            )
        }.sortedByDescending(CaptureHistoryItem::createdAt)
    }

    private suspend fun listManifests(directory: DraftDirectory): Map<DraftId, com.androidvisualqa.files.DraftManifest> {
        val draftsRoot = directory.root.resolve("drafts")
        if (!Files.isDirectory(draftsRoot)) return emptyMap()

        return Files.list(draftsRoot).use { paths ->
            paths.filter { Files.isDirectory(it) }.toList()
                .map { DraftId(it.fileName.toString()) }
                .mapNotNull { id -> draftStore.readDraft(id).getOrNull()?.let { id to it } }
                .associate { it.first to it.second }
        }
    }

    private fun thumbnailPath(directory: DraftDirectory, id: DraftId): Path? =
        listOf(directory.annotatedImagePath(id), directory.originalImagePath(id))
            .firstOrNull(Files::isRegularFile)

    private fun readCommentCount(reportPath: Path, editorStatePath: Path): Int = runCatching {
        if (Files.isRegularFile(reportPath)) {
            val report = JsonConfig.decodeFromString<VisualFeedbackReport>(
                Files.readAllBytes(reportPath).toString(Charsets.UTF_8),
            )
            val linked = report.annotations.sumOf { it.linkedComments.size }
            return if (linked > 0) linked else if (report.feedback.textBody?.isNotBlank() == true) 1 else 0
        }
        if (!Files.isRegularFile(editorStatePath)) return 0
        Files.readAllBytes(editorStatePath).toString(Charsets.UTF_8)
            .lineSequence()
            .filter { it.startsWith("item=") }
            .sumOf { line -> line.substringAfterLast('|').takeIf(String::isNotBlank)?.split(';')?.size ?: 0 }
    }.getOrDefault(0)
}
