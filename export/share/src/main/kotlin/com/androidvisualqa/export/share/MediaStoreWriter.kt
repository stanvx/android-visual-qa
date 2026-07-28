package com.androidvisualqa.export.share

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.androidvisualqa.model.VisualFeedbackReport

/**
 * Saves files to the public Downloads directory via MediaStore (API 29+).
 *
 * ## API 29+ (preferred)
 *
 * Writes using [MediaStore.Downloads.EXTERNAL_CONTENT_URI] with the
 * `IS_PENDING` pattern — set `IS_PENDING = 1` on insert, write the bytes,
 * then set `IS_PENDING = 0` to finalise. No storage permissions needed
 * on API 29+.
 *
 * ## API < 29 (fallback)
 *
 * Writes directly to [Environment.getExternalStoragePublicDirectory].
 *
 * @param context Application [Context] for the [ContentResolver].
 */
public class MediaStoreWriter(private val context: Context) {

    /**
     * Saves a report ZIP to the public Downloads directory.
     *
     * The filename is derived from the report ID: `{reportId}.zip`.
     *
     * @param report   The report metadata (used for display name).
     * @param zipBytes Raw ZIP bytes.
     * @param filename Output filename (e.g. `"report-001.zip"`).
     * @return [Result.success] with the content URI, or [Result.failure].
     */
    public suspend fun saveZipToDownloads(
        report: VisualFeedbackReport,
        zipBytes: ByteArray,
        filename: String,
    ): Result<Uri> = saveFileToDownloads(
        filename = filename,
        mimeType = "application/zip",
        bytes = zipBytes,
    )

    /**
     * Saves an image file to the public Downloads directory.
     *
     * @param filename Output filename (e.g. `"screenshot.png"`).
     * @param mimeType Image MIME type (e.g. `"image/png"`).
     * @param bytes    Raw image bytes.
     * @return [Result.success] with the content URI, or [Result.failure].
     */
    public suspend fun saveImageToDownloads(
        filename: String,
        mimeType: String,
        bytes: ByteArray,
    ): Result<Uri> = saveFileToDownloads(
        filename = filename,
        mimeType = mimeType,
        bytes = bytes,
    )

    private suspend fun saveFileToDownloads(
        filename: String,
        mimeType: String,
        bytes: ByteArray,
    ): Result<Uri> = saveViaMediaStore(filename, mimeType, bytes)

    private suspend fun saveViaMediaStore(
        filename: String,
        mimeType: String,
        bytes: ByteArray,
    ): Result<Uri> = kotlin.runCatching {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to create MediaStore entry for $filename")

        resolver.openOutputStream(uri)?.use { output ->
            output.write(bytes)
        } ?: throw IllegalStateException("Failed to open output stream for $filename")

        // Finalise — mark as no longer pending
        val finalValues = ContentValues().apply {
            put(MediaStore.Downloads.IS_PENDING, 0)
        }
        resolver.update(uri, finalValues, null, null)

        uri
    }
}
