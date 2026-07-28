package com.androidvisualqa.export.share

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

/**
 * Writes bytes to a FileProvider-visible location and returns a `content://` URI.
 *
 * ## Usage
 *
 * ```kotlin
 * val writer = FileProviderWriter(context)
 * val uri = writer.writeBytesToCache("exports", "report.zip", zipBytes).getOrThrow()
 * // share uri via ShareIntentBuilder
 * ```
 *
 * The FileProvider must be declared in the host app's `AndroidManifest.xml` with
 * an authority matching `${context.packageName}.fileprovider`. The `file_paths.xml`
 * resource must include `<cache-path>` and `<files-path>` entries as needed.
 *
 * @param context   Application [Context] for resolving cache/files dirs.
 * @param authority The FileProvider authority (defaults to `{packageName}.fileprovider`).
 */
public class FileProviderWriter(
    private val context: Context,
    private val authority: String = "${context.packageName}.fileprovider",
) {

    /**
     * Writes [bytes] to a subdirectory of the app's cache directory and returns
     * a `content://` URI via [FileProvider.getUriForFile].
     *
     * The file is placed at `context.cacheDir / subdir / filename`.
     *
     * @param subdir   Relative subdirectory under [Context.cacheDir].
     * @param filename File name within the subdirectory.
     * @param bytes    Raw bytes to write.
     * @return [Result.success] with the FileProvider URI, or [Result.failure].
     */
    public fun writeBytesToCache(
        subdir: String,
        filename: String,
        bytes: ByteArray,
    ): Result<Uri> = runCatching {
        val dir = File(context.cacheDir, subdir).also { it.mkdirs() }
        val file = File(dir, filename)
        file.writeBytes(bytes)
        FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Writes [bytes] to a subdirectory of the app's files directory and returns
     * a `content://` URI via [FileProvider.getUriForFile].
     *
     * The file is placed at `context.filesDir / subdir / filename`.
     *
     * @param subdir   Relative subdirectory under [Context.filesDir].
     * @param filename File name within the subdirectory.
     * @param bytes    Raw bytes to write.
     * @return [Result.success] with the FileProvider URI, or [Result.failure].
     */
    public fun writeBytesToFiles(
        subdir: String,
        filename: String,
        bytes: ByteArray,
    ): Result<Uri> = runCatching {
        val dir = File(context.filesDir, subdir).also { it.mkdirs() }
        val file = File(dir, filename)
        file.writeBytes(bytes)
        FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Writes [bytes] to a complete [relativePath] under the app's cache directory.
     *
     * The file is placed at `context.cacheDir / relativePath`.
     *
     * @param relativePath Relative file path under [Context.cacheDir] (may include subdirectories).
     * @param bytes        Raw bytes to write.
     * @return [Result.success] with the FileProvider URI, or [Result.failure].
     */
    public fun writeBytes(
        relativePath: String,
        bytes: ByteArray,
    ): Result<Uri> = runCatching {
        val file = File(context.cacheDir, relativePath).also {
            it.parentFile?.mkdirs()
        }
        file.writeBytes(bytes)
        FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Recursively deletes the entire [Context.cacheDir].
     *
     * @return [Result.success] on completion (even if no files existed).
     */
    public fun clearCache(): Result<Unit> = runCatching {
        deleteRecursively(context.cacheDir)
    }

    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        file.delete()
    }
}
