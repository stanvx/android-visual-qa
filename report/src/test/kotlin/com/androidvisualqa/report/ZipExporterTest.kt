package com.androidvisualqa.report

import com.androidvisualqa.model.ReportStatus
import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.model.capture.CaptureFrame
import com.androidvisualqa.model.capture.CaptureMode
import com.androidvisualqa.model.capture.CaptureSession
import com.androidvisualqa.model.capture.ScreenshotMethod
import com.androidvisualqa.model.feedback.FeedbackEvidence
import com.androidvisualqa.model.ids.ReportId
import com.androidvisualqa.model.privacy.PrivacyEvidence
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipExporterTest {
    @TempDir
    lateinit var tempDir: Path

    private val exporter = ZipExporter()

    @Test
    fun `export produces valid zip with all entries`() {
        val originalPng = tempDir.resolve("original.png")
        val annotatedPng = tempDir.resolve("annotated.png")
        val zipTarget = tempDir.resolve("report.zip")

        // Create small dummy PNGs (not valid PNG headers, just test files with content)
        val pngHeader = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte())
        Files.write(originalPng, pngHeader + byteArrayOf(0x01, 0x02, 0x03))
        Files.write(annotatedPng, pngHeader + byteArrayOf(0x04, 0x05, 0x06))

        val result = exporter.export(fixedReport, originalPng, annotatedPng, zipTarget)
        assertTrue(result.isSuccess)

        // Read back the zip and verify entries
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(Files.newInputStream(zipTarget)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readAllBytes()
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        assertEquals(5, entries.size)
        assertTrue(entries.containsKey("manifest.json"))
        assertTrue(entries.containsKey("report.json"))
        assertTrue(entries.containsKey("report.md"))
        assertTrue(entries.containsKey("original.png"))
        assertTrue(entries.containsKey("annotated.png"))

        // Verify original.png content is preserved
        val expectedOriginal = pngHeader + byteArrayOf(0x01, 0x02, 0x03)
        val expectedAnnotated = pngHeader + byteArrayOf(0x04, 0x05, 0x06)
        assertArrayEquals(expectedOriginal, entries["original.png"])
        assertArrayEquals(expectedAnnotated, entries["annotated.png"])

        // Verify manifest.json lists all four files
        val manifestJson = entries["manifest.json"]!!.decodeToString()
        assertTrue(manifestJson.contains("report.json"))
        assertTrue(manifestJson.contains("report.md"))
        assertTrue(manifestJson.contains("original.png"))
        assertTrue(manifestJson.contains("annotated.png"))
    }

    @Test
    fun `export failure returns failure`() {
        val missingPng = tempDir.resolve("nonexistent.png")
        val annotatedPng = tempDir.resolve("annotated.png")
        Files.write(annotatedPng, byteArrayOf(0, 1, 2))
        val zipTarget = tempDir.resolve("report.zip")

        val result = exporter.export(fixedReport, missingPng, annotatedPng, zipTarget)
        assertTrue(result.isFailure)
    }

    companion object {
        private val now = Instant.parse("2026-07-28T12:00:00Z")
    }

    private val fixedReport = VisualFeedbackReport(
            schemaVersion = 1,
            reportId = ReportId("zip-test-id"),
            createdAt = now,
            status = ReportStatus.Saved,
            capture = CaptureSession(
                sessionId = "sess-1",
                startedAt = now,
                triggerSource = com.androidvisualqa.model.capture.TriggerSource.AccessibilityOverlay,
                captureMode = CaptureMode.Still,
            ),
            frame = CaptureFrame(
                displayId = 0,
                windowId = 123,
                packageName = "com.example.test",
                widthPx = 1080,
                heightPx = 2400,
                density = 2.0f,
                rotationDegrees = 0,
                screenshotMethod = ScreenshotMethod.AccessibilityWindow,
                monotonicTimestamp = 1000L,
                wallClockTimestamp = now,
            ),
            feedback = FeedbackEvidence("Test report for zip"),
            privacy = PrivacyEvidence(),
        )
}
