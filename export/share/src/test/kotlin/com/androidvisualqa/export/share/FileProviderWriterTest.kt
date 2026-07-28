package com.androidvisualqa.export.share

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Robolectric tests for [FileProviderWriter].
 *
 * Note: FileProvider is declared in the `:app` module's manifest,
 * not in this library module. `getUriForFile` will throw
 * [IllegalArgumentException] when no FileProvider is registered
 * for the given authority. These tests verify that:
 * - Files are written to the correct location
 * - The error from getUriForFile is handled gracefully
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [30])
class FileProviderWriterTest {

    private lateinit var writer: FileProviderWriter
    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        writer = FileProviderWriter(context, "com.androidvisualqa.app.test.fileprovider")
    }

    @Test
    fun `writeBytesToCache writes file to cache dir and file exists`() {
        val bytes = "hello world".toByteArray()
        val result: Result<Uri> = kotlin.runCatching {
            writer.writeBytesToCache("exports", "test.zip", bytes).getOrThrow()
        }

        // The file should exist even if URI resolution fails
        val writtenFile = File(context.cacheDir, "exports/test.zip")
        assertTrue("File should exist at cacheDir/exports/test.zip", writtenFile.exists())
        assertEquals(bytes.size.toLong(), writtenFile.length())

        // URI resolution may fail without a registered FileProvider
        if (result.isSuccess) {
            assertEquals("content", result.getOrThrow().scheme)
        }
    }

    @Test
    fun `writeBytesToFiles writes file to files dir and file exists`() {
        val bytes = "file content".toByteArray()
        val result: Result<Uri> = kotlin.runCatching {
            writer.writeBytesToFiles("exports", "test.zip", bytes).getOrThrow()
        }

        val writtenFile = File(context.filesDir, "exports/test.zip")
        assertTrue("File should exist at filesDir/exports/test.zip", writtenFile.exists())
        assertEquals(bytes.size.toLong(), writtenFile.length())

        if (result.isSuccess) {
            assertEquals("content", result.getOrThrow().scheme)
        }
    }

    @Test
    fun `writeBytes writes to relative path under cache`() {
        val bytes = "nested path".toByteArray()
        val result: Result<Uri> = kotlin.runCatching {
            writer.writeBytes("exports/nested/test.zip", bytes).getOrThrow()
        }

        val writtenFile = File(context.cacheDir, "exports/nested/test.zip")
        assertTrue("File should exist", writtenFile.exists())
        assertEquals(bytes.size.toLong(), writtenFile.length())

        if (result.isSuccess) {
            assertEquals("content", result.getOrThrow().scheme)
        }
    }

    @Test
    fun `clearCache deletes cache files`() {
        // Write something first
        kotlin.runCatching { writer.writeBytesToCache("subdir", "file.txt", "data".toByteArray()) }

        val result = writer.clearCache()
        assertTrue(result.isSuccess)

        // Cache dir should exist but the subdir/file should be gone
        val subdir = File(context.cacheDir, "subdir")
        assertFalse("Cache subdir should be deleted", subdir.exists())
    }

    @Test
    fun `writeBytesToCache fails gracefully when FileProvider not registered`() {
        val bytes = "test".toByteArray()
        val result = writer.writeBytesToCache("somedir", "f.zip", bytes)

        // No FileProvider registered — expect IllegalArgumentException
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
