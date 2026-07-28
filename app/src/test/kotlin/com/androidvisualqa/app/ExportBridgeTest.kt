package com.androidvisualqa.app

import com.androidvisualqa.app.export.ExportBridge
import com.androidvisualqa.files.DraftDirectory
import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.model.ids.ReportId
import com.androidvisualqa.testing.testReport
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * Unit tests for [ExportBridge].
 *
 * Uses a hand-rolled fake [Context] to satisfy Android API requirements
 * without Robolectric. Draft files are created in a temp directory.
 *
 * ponytail: validates that the bridge delegates to
 * [ReportShareOrchestrator] by checking that the returned [Result] is a
 * [Result.failure] because FileProvider / MediaStore are unavailable in a
 * plain unit test environment.
 */
class ExportBridgeTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var context: FakeContext
    private lateinit var draftDirectory: DraftDirectory
    private lateinit var bridge: ExportBridge
    private lateinit var report: VisualFeedbackReport

    @BeforeEach
    fun setUp() {
        context = FakeContext(tempDir.toFile())
        draftDirectory = DraftDirectory(tempDir.resolve("drafts"))
        bridge = ExportBridge(context, draftDirectory)
        report = testReport(reportId = ReportId("test-report-001"))

        // Create a draft directory with the required files
        val draftId = com.androidvisualqa.model.ids.DraftId(report.capture.sessionId)
        val draftPath = draftDirectory.draftPath(draftId).also { Files.createDirectories(it) }

        // Write placeholder PNG bytes (valid PNG header)
        Files.write(draftPath.resolve("original.png"), fakePngBytes())
        Files.write(draftPath.resolve("annotated.png"), fakePngBytes())
    }

    @Test
    fun `exportAndShare delegates to orchestrator and returns failure in unit test`() = runBlocking {
        // ReportShareOrchestrator.shareAsZip catches the NotImplementedError
        // from FileProvider inside its own runCatching, so bridge returns a
        // Result.failure directly — no exception thrown.
        val result = bridge.exportAndShare(report)

        assertTrue(result.isFailure) {
            "exportAndShare must fail in unit test without platform mocking, " +
                "got: ${result.getOrNull()}"
        }
    }

    @Test
    fun `saveToDownloads delegates to orchestrator and returns failure in unit test`() = runBlocking {
        val result = bridge.saveToDownloads(report)

        assertTrue(result.isFailure) {
            "saveToDownloads should fail in unit test because MediaStore " +
                "is not available, got: ${result.getOrNull()}"
        }
    }

    private fun fakePngBytes(): ByteArray = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, // PNG magic
        0x0D, 0x0A, 0x1A, 0x0A, // CR+LF+EOF+LF
    )
}
