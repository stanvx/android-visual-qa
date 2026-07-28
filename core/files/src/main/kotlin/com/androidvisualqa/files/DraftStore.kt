package com.androidvisualqa.files

import com.androidvisualqa.model.ids.DraftId
import kotlinx.datetime.Instant
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Stream

/**
 * Metadata for a draft stored on disk.
 *
 * @property draftId The draft's unique identifier.
 * @property createdAt When the draft was created.
 * @property originalSha256 SHA-256 of the original screenshot, or null if not yet written.
 * @property annotatedSha256 SHA-256 of the annotated image, or null if not yet written.
 * @property reportSchemaVersion Schema version of the report this draft belongs to.
 * @property captureState The last recorded capture state name.
 */
public data class DraftManifest(
    val draftId: DraftId,
    val createdAt: Instant,
    val originalSha256: String? = null,
    val annotatedSha256: String? = null,
    val reportSchemaVersion: Int,
    val captureState: String,
)

/**
 * Abstract store for draft CRUD operations.
 */
public interface DraftStore {
    /** Creates a new draft directory and returns its [DraftId]. */
    public suspend fun createDraft(): Result<DraftId>

    /** Writes the original screenshot bytes for [id]. */
    public suspend fun writeOriginal(id: DraftId, bytes: ByteArray): Result<Path>

    /** Writes the annotated image bytes for [id]. */
    public suspend fun writeAnnotated(id: DraftId, bytes: ByteArray): Result<Path>

    /** Writes the draft manifest JSON. */
    public suspend fun writeManifest(id: DraftId, manifest: DraftManifest): Result<Path>

    /** Reads the draft manifest, or returns null if the draft does not exist. */
    public suspend fun readDraft(id: DraftId): Result<DraftManifest?>

    /** Deletes the entire draft directory and all its contents. */
    public suspend fun deleteDraft(id: DraftId): Result<Unit>
}

/**
 * [DraftStore] implementation backed by the local filesystem.
 *
 * @param directory The [DraftDirectory] root that defines the path layout.
 */
public class FileSystemDraftStore(public val directory: DraftDirectory) : DraftStore {

    override suspend fun createDraft(): Result<DraftId> = runCatching {
        val id = DraftIdGenerator.new()
        Files.createDirectories(directory.draftPath(id))
        id
    }

    override suspend fun writeOriginal(id: DraftId, bytes: ByteArray): Result<Path> = runCatching {
        val target = directory.originalImagePath(id)
        AtomicFileWriter.writeAtomically(target, bytes).getOrThrow()
        target
    }

    override suspend fun writeAnnotated(id: DraftId, bytes: ByteArray): Result<Path> = runCatching {
        val target = directory.annotatedImagePath(id)
        AtomicFileWriter.writeAtomically(target, bytes).getOrThrow()
        target
    }

    override suspend fun writeManifest(id: DraftId, manifest: DraftManifest): Result<Path> = runCatching {
        val target = directory.manifestPath(id)
        AtomicFileWriter.writeTextAtomically(target, manifestToString(manifest)).getOrThrow()
        target
    }

    override suspend fun readDraft(id: DraftId): Result<DraftManifest?> = runCatching {
        val manifestFile = directory.manifestPath(id)
        if (!Files.exists(manifestFile)) return@runCatching null

        val text = Files.readString(manifestFile)
        manifestFromString(text)
    }

    override suspend fun deleteDraft(id: DraftId): Result<Unit> = runCatching {
        val dir = directory.draftPath(id)
        if (Files.exists(dir)) {
            deleteRecursively(dir)
        }
    }

    // --- Internal helpers (simple JSON-like format, no serialization dep in :core:files) ---

    /**
     * Serialises a [DraftManifest] to a simple line-based text format.
     *
     * Format (one field per line, key=value):
     * ```
     * draftId=draft-<uuid>
     * createdAt=2026-07-28T10:00:00Z
     * originalSha256=<hex>
     * annotatedSha256=<hex>
     * reportSchemaVersion=1
     * captureState=Complete
     * ```
     *
     * ponytail: This is intentionally a trivial key=value format rather than pulling
     * in kotlinx-serialization for `:core:files`. If the manifest grows beyond 6 fields,
     * replace with kotlinx-serialization JSON.
     */
    private fun manifestToString(m: DraftManifest): String = buildString {
        appendLine("draftId=${m.draftId.value}")
        appendLine("createdAt=${m.createdAt}")
        m.originalSha256?.let { appendLine("originalSha256=$it") }
        m.annotatedSha256?.let { appendLine("annotatedSha256=$it") }
        appendLine("reportSchemaVersion=${m.reportSchemaVersion}")
        appendLine("captureState=${m.captureState}")
    }

    private fun manifestFromString(text: String): DraftManifest {
        val map = text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && '=' in it }
            .associate { line ->
                val eq = line.indexOf('=')
                line.substring(0, eq) to line.substring(eq + 1)
            }

        return DraftManifest(
            draftId = DraftId(map.getValue("draftId")),
            createdAt = Instant.parse(map.getValue("createdAt")),
            originalSha256 = map["originalSha256"],
            annotatedSha256 = map["annotatedSha256"],
            reportSchemaVersion = map.getValue("reportSchemaVersion").toInt(),
            captureState = map.getValue("captureState"),
        )
    }

    private fun deleteRecursively(dir: Path) {
        if (Files.isDirectory(dir)) {
            Files.walkFileTree(dir, object : java.nio.file.SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): java.nio.file.FileVisitResult {
                    Files.delete(file)
                    return java.nio.file.FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(d: Path, exc: IOException?): java.nio.file.FileVisitResult {
                    Files.delete(d)
                    return java.nio.file.FileVisitResult.CONTINUE
                }
            })
        }
    }
}
