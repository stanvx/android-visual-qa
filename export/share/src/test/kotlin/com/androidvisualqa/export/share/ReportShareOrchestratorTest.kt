package com.androidvisualqa.export.share

import android.content.Intent
import android.net.Uri
import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.report.ZipExporter
import com.androidvisualqa.testing.testReport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric tests for [ReportShareOrchestrator].
 *
 * Uses the real [ZipExporter] from `:report`. The share-as-ZIP path
 * depends on [FileProviderWriter] which requires a registered FileProvider
 * in the host app's manifest — in this library-only test environment,
 * that path returns [Result.failure] with [IllegalArgumentException].
 * The save-to-Downloads path works because Robolectric's shadow
 * [ContentResolver] supports MediaStore operations.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [30])
class ReportShareOrchestratorTest {

    private lateinit var orchestrator: ReportShareOrchestrator
    private lateinit var report: VisualFeedbackReport

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        val fileProviderWriter = FileProviderWriter(context)
        val mediaStoreWriter = MediaStoreWriter(context)
        val zipExporter = ZipExporter()
        orchestrator = ReportShareOrchestrator(fileProviderWriter, mediaStoreWriter, zipExporter)
        report = testReport()
    }

    @Test
    fun `shareAsZip fails gracefully when FileProvider not available`() = runBlocking {
        // In a library test environment with no FileProvider, this is expected
        val result = orchestrator.shareAsZip(report, fakePngBytes(), fakePngBytes())

        // The temp ZIP should have been produced but the FileProvider URI fails
        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull() is IllegalArgumentException ||
            result.exceptionOrNull()?.message?.contains("FileProvider") == true
        )
    }

    @Test
    fun `saveZipToDownloads returns a URI via MediaStore`() = runBlocking {
        val result = orchestrator.saveZipToDownloads(report, fakePngBytes(), fakePngBytes())

        assertTrue(result.isSuccess)
        val uri = result.getOrThrow()
        assertNotNull(uri)
    }

    @Test
    fun `saveZipToDownloads URI has content scheme`() = runBlocking {
        val result = orchestrator.saveZipToDownloads(report, fakePngBytes(), fakePngBytes())

        assertTrue(result.isSuccess)
        val uri = result.getOrThrow()
        assertTrue(
            "Expected content or file scheme, got ${uri.scheme}",
            uri.scheme?.startsWith("content") == true || uri.scheme?.startsWith("file") == true
        )
    }

    private fun fakePngBytes(): ByteArray = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47,
        0x0D, 0x0A, 0x1A, 0x0A,
    )
}
