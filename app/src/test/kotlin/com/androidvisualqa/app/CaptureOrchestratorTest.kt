package com.androidvisualqa.app

import com.androidvisualqa.annotation.AnnotationId
import com.androidvisualqa.annotation.RectangleAnnotation
import com.androidvisualqa.capture.api.CapturedFrame
import com.androidvisualqa.files.DraftDirectory
import com.androidvisualqa.files.FileSystemDraftStore
import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import com.androidvisualqa.geometry.Rotation
import com.androidvisualqa.model.ReportStatus
import com.androidvisualqa.model.capture.CaptureFrame
import com.androidvisualqa.model.capture.CaptureSession
import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.capture.ScreenshotMethod
import com.androidvisualqa.model.capture.TriggerSource
import com.androidvisualqa.model.capture.CaptureMode
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.model.ids.NodeId
import com.androidvisualqa.report.FileSystemReportHistoryIndex
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Unit tests for [CaptureOrchestrator].
 *
 * Uses hand-rolled fakes for the capture lambda and real
 * [FileSystemDraftStore] / [FileSystemReportHistoryIndex] backed by a temp dir.
 */
class CaptureOrchestratorTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var draftStore: FileSystemDraftStore
    private lateinit var draftDirectory: DraftDirectory
    private lateinit var reportHistory: FileSystemReportHistoryIndex
    private lateinit var orchestrator: CaptureOrchestrator
    private val clock = Clock.System

    @BeforeEach
    fun setUp() {
        draftDirectory = DraftDirectory(tempDir)
        draftStore = FileSystemDraftStore(draftDirectory)
        reportHistory = FileSystemReportHistoryIndex(tempDir.resolve("history.jsonl"))
        orchestrator = CaptureOrchestrator()
    }

    @Test
    fun `startCapture failure on capture lambda error returns failure`() = runTest {
        val captureLambda: suspend () -> Result<CaptureResult> = {
            Result.failure(IllegalStateException("Capture failed"))
        }
        val pkgLambda: suspend () -> String = { "com.test" }

        val result = orchestrator.startCapture(captureLambda, pkgLambda, draftStore, reportHistory)

        assertTrue(result.isFailure)
        assertEquals("Capture failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `startCapture happy path persists draft with original`() = runTest {
        val frame = CapturedFrame(
            displayId = 0,
            widthPx = 480,
            heightPx = 800,
            rotation = Rotation.ROTATION_0,
            capturedAt = clock.now(),
        )
        val pngBytes = fakePngBytes()
        val captureLambda: suspend () -> Result<CaptureResult> = {
            Result.success(CaptureResult(frame = frame, pngBytes = pngBytes))
        }
        val pkgLambda: suspend () -> String = { "com.test" }

        val result = orchestrator.startCapture(captureLambda, pkgLambda, draftStore, reportHistory)

        assertTrue(result.isSuccess) { "Expected success, got: ${result.exceptionOrNull()}" }
        val draftId = result.getOrThrow()

        // Verify draft directory was created
        val draftPath = draftDirectory.draftPath(draftId)
        assertTrue(java.nio.file.Files.exists(draftPath))

        // Verify original.png was written
        val originalPath = draftDirectory.originalImagePath(draftId)
        assertTrue(java.nio.file.Files.exists(originalPath))
        val writtenBytes = java.nio.file.Files.readAllBytes(originalPath)
        assertTrue(writtenBytes.contentEquals(pngBytes))

        // Verify manifest was written
        val manifest = draftStore.readDraft(draftId).getOrThrow()
        assertNotNull(manifest)
        assertEquals("Captured", manifest?.captureState)
        assertEquals(draftId, manifest?.draftId)
    }

    @Test
    fun `finishDraft produces report with one selection when lasso contains a node`() = runTest {
        // First, capture a draft
        val frame = CapturedFrame(
            displayId = 0,
            widthPx = 480,
            heightPx = 800,
            rotation = Rotation.ROTATION_0,
            capturedAt = clock.now(),
        )
        val captureLambda: suspend () -> Result<CaptureResult> = {
            Result.success(CaptureResult(frame = frame, pngBytes = fakePngBytes()))
        }
        val pkgLambda: suspend () -> String = { "com.test" }
        val draftResult = orchestrator.startCapture(captureLambda, pkgLambda, draftStore, reportHistory)
        assertTrue(draftResult.isSuccess)
        val draftId = draftResult.getOrThrow()

        // Create a node that the rectangle fully contains
        val node = NodeSnapshot(
            nodeId = NodeId("child-1"),
            windowId = 42,
            boundsLeft = 100,
            boundsTop = 100,
            boundsRight = 300,
            boundsBottom = 300,
            text = "Tap me",
            isClickable = true,
        )

        val screenBounds = Bounds(
            left = 0.0, top = 0.0, right = 480.0, bottom = 800.0,
            space = CoordinateSpace.ScreenPx,
        )

        val captureFrame = CaptureFrame(
            displayId = 0, windowId = 42, packageName = "com.test",
            activityTitle = null,
            widthPx = 480, heightPx = 800,
            density = 2f, rotationDegrees = 0,
            systemBarsTop = 0, systemBarsBottom = 0,
            systemBarsLeft = 0, systemBarsRight = 0,
            windowBoundsLeft = 0, windowBoundsTop = 0,
            windowBoundsRight = 480, windowBoundsBottom = 800,
            contentBoundsLeft = 0, contentBoundsTop = 0,
            contentBoundsRight = 480, contentBoundsBottom = 800,
            screenshotMethod = ScreenshotMethod.AccessibilityWindow,
            monotonicTimestamp = 1000L,
            wallClockTimestamp = clock.now(),
        )

        val session = CaptureSession(
            sessionId = "session-1",
            startedAt = clock.now(),
            triggerSource = TriggerSource.QuickSettingsTile,
            captureMode = CaptureMode.Still,
        )

        val rectangle = RectangleAnnotation(
            id = AnnotationId("rect-1"),
            left = 50f, top = 50f,
            right = 350f, bottom = 350f,
            color = 0xFF0000FFL.toLong(),
        )

        val reportResult = orchestrator.finishDraft(
            draftId = draftId,
            rectangle = rectangle,
            feedback = "This button is misaligned",
            candidates = listOf(node),
            screenBounds = screenBounds,
            frame = captureFrame,
            session = session,
            draftStore = draftStore,
            reportHistory = reportHistory,
            draftDirectory = draftDirectory,
        )

        assertTrue(reportResult.isSuccess) { "Expected success, got: ${reportResult.exceptionOrNull()}" }
        val report = reportResult.getOrThrow()

        assertEquals("com.test", report.frame.packageName)
        assertEquals(ReportStatus.Saved, report.status)
        assertEquals(1, report.selections.size)
        assertEquals(1, report.annotations.size)
        assertEquals("This button is misaligned", report.feedback.textBody)

        val selection = report.selections.first()
        assertNotNull(selection.chosenNodeId)
        assertEquals(NodeId("child-1"), selection.chosenNodeId)
        assertTrue(selection.confidence > 0.0)
    }

    @Test
    fun `finishDraft writes report files and history`() = runTest {
        val frame = CapturedFrame(
            displayId = 0,
            widthPx = 480,
            heightPx = 800,
            rotation = Rotation.ROTATION_0,
            capturedAt = clock.now(),
        )
        val captureLambda: suspend () -> Result<CaptureResult> = {
            Result.success(CaptureResult(frame = frame, pngBytes = fakePngBytes()))
        }
        val pkgLambda: suspend () -> String = { "com.test" }
        val draftResult = orchestrator.startCapture(captureLambda, pkgLambda, draftStore, reportHistory)
        assertTrue(draftResult.isSuccess)
        val draftId = draftResult.getOrThrow()

        val candidate = NodeSnapshot(
            nodeId = NodeId("node-1"),
            windowId = 42,
            boundsLeft = 100, boundsTop = 100,
            boundsRight = 200, boundsBottom = 200,
        )

        val captureFrame = CaptureFrame(
            displayId = 0, windowId = 42, packageName = "com.test",
            activityTitle = null,
            widthPx = 480, heightPx = 800,
            density = 2f, rotationDegrees = 0,
            systemBarsTop = 0, systemBarsBottom = 0,
            systemBarsLeft = 0, systemBarsRight = 0,
            windowBoundsLeft = 0, windowBoundsTop = 0,
            windowBoundsRight = 480, windowBoundsBottom = 800,
            contentBoundsLeft = 0, contentBoundsTop = 0,
            contentBoundsRight = 480, contentBoundsBottom = 800,
            screenshotMethod = ScreenshotMethod.AccessibilityWindow,
            monotonicTimestamp = 1000L,
            wallClockTimestamp = clock.now(),
        )

        val session = CaptureSession(
            sessionId = "session-2",
            startedAt = clock.now(),
            triggerSource = TriggerSource.QuickSettingsTile,
            captureMode = CaptureMode.Still,
        )

        val rect = RectangleAnnotation(
            id = AnnotationId("rect-1"),
            left = 80f, top = 80f,
            right = 220f, bottom = 220f,
            color = 0xFF0000FFL.toLong(),
        )

        val reportResult = orchestrator.finishDraft(
            draftId = draftId,
            rectangle = rect,
            feedback = "Looks good",
            candidates = listOf(candidate),
            screenBounds = Bounds(0.0, 0.0, 480.0, 800.0, CoordinateSpace.ScreenPx),
            frame = captureFrame,
            session = session,
            draftStore = draftStore,
            reportHistory = reportHistory,
            draftDirectory = draftDirectory,
        )

        assertTrue(reportResult.isSuccess)

        // Check report files exist
        assertTrue(java.nio.file.Files.exists(draftDirectory.reportJsonPath(draftId)))
        assertTrue(java.nio.file.Files.exists(draftDirectory.reportMarkdownPath(draftId)))

        // Check manifest was updated
        val manifest = draftStore.readDraft(draftId).getOrThrow()
        assertEquals("Complete", manifest?.captureState)

        // Check history was written
        val entries = reportHistory.list()
        assertEquals(1, entries.size)
        assertEquals(reportResult.getOrThrow().reportId, entries.first().reportId)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    /**
     * Returns non-empty PNG-like bytes for test purposes.
     *
     * ponytail: fake PNG bytes since [DraftStore.writeOriginal] needs real
     * bytes to write to disk. The exact PNG structure is not validated.
     */
    private fun fakePngBytes(): ByteArray = "fake-png-content".encodeToByteArray()
}
