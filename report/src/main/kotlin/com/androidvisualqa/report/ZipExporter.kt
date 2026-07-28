package com.androidvisualqa.report

import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.model.serialization.JsonConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Produces a ZIP bundle containing the report JSON, Markdown, and image attachments.
 *
 * ZIP layout:
 *   manifest.json   – file listing with schema version
 *   report.json     – serialized [VisualFeedbackReport]
 *   report.md       – Markdown document
 *   original.png    – original screenshot (streamed from disk)
 *   annotated.png   – annotated screenshot (streamed from disk)
 */
public class ZipExporter {

    @Serializable
    private data class Manifest(
        val schemaVersion: Int = 1,
        val reportId: String,
        val createdAt: String,
        val files: List<String>,
    )

    /**
     * Creates a ZIP archive at [target] containing the report artifacts.
     *
     * @param report        The report to export.
     * @param originalPng   Path to the original screenshot PNG.
     * @param annotatedPng  Path to the annotated screenshot PNG.
     * @param target        Destination path for the ZIP file (including .zip extension).
     * @return [Result.success] with [target] on success, [Result.failure] with [IOException] on failure.
     */
    public fun export(
        report: VisualFeedbackReport,
        originalPng: Path,
        annotatedPng: Path,
        target: Path,
    ): Result<Path> {
        return try {
            val parent = target.parent
            Files.createDirectories(parent)

            // Write to a temp sibling to avoid torn output
            val tmp = parent.resolve(".${target.fileName}.tmp")
            Files.newOutputStream(tmp).use { rawOut ->
                ZipOutputStream(rawOut.buffered()).use { zos ->
                    // report.json
                    val reportJson = JsonConfig.encodeToString(report)
                    putEntry(zos, "report.json", reportJson.toByteArray())

                    // report.md
                    val markdown = MarkdownReportWriter().write(report)
                    putEntry(zos, "report.md", markdown.toByteArray())

                    // original.png – stream from disk
                    copyFileEntry(zos, "original.png", originalPng)

                    // annotated.png – stream from disk
                    copyFileEntry(zos, "annotated.png", annotatedPng)

                    // manifest.json last so it's complete
                    val manifest = Manifest(
                        reportId = report.reportId.value,
                        createdAt = report.createdAt.toString(),
                        files = listOf("report.json", "report.md", "original.png", "annotated.png"),
                    )
                    val manifestJson = JsonConfig.encodeToString(manifest)
                    putEntry(zos, "manifest.json", manifestJson.toByteArray())
                }
            }

            // Atomically replace target
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: UnsupportedOperationException) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING)
            }

            Result.success(target)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    private fun putEntry(zos: ZipOutputStream, name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        entry.size = data.size.toLong()
        zos.putNextEntry(entry)
        zos.write(data)
        zos.closeEntry()
    }

    private fun copyFileEntry(zos: ZipOutputStream, name: String, source: Path) {
        val size = Files.size(source)
        val entry = ZipEntry(name)
        entry.size = size
        zos.putNextEntry(entry)
        Files.newInputStream(source).use { input ->
            input.transferTo(zos as OutputStream)
        }
        zos.closeEntry()
    }
}
