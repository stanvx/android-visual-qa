package com.androidvisualqa.files

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Pure JVM atomic file writer.
 *
 * Writes to a temporary sibling file, then atomically renames to the target.
 * Falls back to a non-atomic replace when the underlying filesystem does not
 * support atomic moves (e.g. default tmpfs on some Linux configurations).
 */
public object AtomicFileWriter {

    /**
     * Writes [bytes] to [target] atomically.
     *
     * @param target The final destination path.
     * @param bytes The raw bytes to write.
     * @return [Result.success] on success, [Result.failure] with an [IOException] on error.
     */
    public suspend fun writeAtomically(target: Path, bytes: ByteArray): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val parent = target.parent
                Files.createDirectories(parent)

                val tmp = parent.resolve(".${target.fileName}.tmp")
                Files.write(tmp, bytes)

                // ponytail: atomic move fails on some tmpfs → fall back to REPLACE_EXISTING
                try {
                    Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                } catch (_: UnsupportedOperationException) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING)
                }

                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(e)
            }
        }

    /**
     * Convenience wrapper that encodes [text] with [charset] and delegates to [writeAtomically].
     */
    public suspend fun writeTextAtomically(
        target: Path,
        text: String,
        charset: Charset = StandardCharsets.UTF_8,
    ): Result<Unit> = writeAtomically(target, text.toByteArray(charset))
}
