package com.androidvisualqa.export.share

import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.testing.testReport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric tests for [MediaStoreWriter].
 *
 * Robolectric's shadow [android.content.ContentResolver] provides
 * a working fake for [android.provider.MediaStore] insert/query.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [30])
class MediaStoreWriterTest {

    private lateinit var writer: MediaStoreWriter
    private lateinit var report: VisualFeedbackReport

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        writer = MediaStoreWriter(context)
        report = testReport()
    }

    @Test
    fun `saveZipToDownloads writes a ZIP and returns a non-null URI`() = runBlocking {
        val zipBytes = "fake-zip-content".toByteArray()
        val result = writer.saveZipToDownloads(zipBytes, "test-report.zip")

        assertTrue(result.isSuccess)
        val uri = result.getOrThrow()
        assertNotNull(uri)
        assertTrue(uri.scheme?.startsWith("content") == true ||
            uri.scheme?.startsWith("file") == true)
    }

    @Test
    fun `saveImageToDownloads writes an image and returns a non-null URI`() = runBlocking {
        val imageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val result = writer.saveImageToDownloads("screenshot.png", "image/png", imageBytes)

        assertTrue(result.isSuccess)
        val uri = result.getOrThrow()
        assertNotNull(uri)
    }

    @Test
    fun `saveZipToDownloads with empty bytes produces valid URI`() = runBlocking {
        val result = writer.saveZipToDownloads(ByteArray(0), "empty.zip")

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrThrow())
    }
}
