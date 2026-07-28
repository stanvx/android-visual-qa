package com.androidvisualqa.app

import com.androidvisualqa.annotation.RectangleAnnotation
import com.androidvisualqa.capture.api.CapturedFrame
import com.androidvisualqa.files.DraftDirectory
import com.androidvisualqa.files.FileSystemDraftStore
import com.androidvisualqa.files.Hashing
import com.androidvisualqa.files.AtomicFileWriter
import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import com.androidvisualqa.geometry.Point
import com.androidvisualqa.geometry.Polygon
import com.androidvisualqa.matching.DecisionPolicy
import com.androidvisualqa.matching.MatchingEngine
import com.androidvisualqa.matching.MatchingInput
import com.androidvisualqa.model.ReportStatus
import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.model.annotation.AnnotationEvidence
import com.androidvisualqa.model.annotation.AnnotationTool
import com.androidvisualqa.model.attachment.AttachmentRef
import com.androidvisualqa.model.capture.CaptureFrame
import com.androidvisualqa.model.capture.CaptureSession
import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.capture.ScreenshotMethod
import com.androidvisualqa.model.capture.CaptureMode
import com.androidvisualqa.model.capture.TriggerSource
import com.androidvisualqa.model.feedback.FeedbackEvidence
import com.androidvisualqa.model.ids.AttachmentId
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.model.privacy.PrivacyEvidence
import com.androidvisualqa.model.privacy.SecureWindowResult
import com.androidvisualqa.model.selection.ComponentSelection
import com.androidvisualqa.model.selection.EvidenceSource
import com.androidvisualqa.model.selection.SelectionChoiceType
import com.androidvisualqa.report.AssemblyInput
import com.androidvisualqa.report.FileSystemReportHistoryIndex
import com.androidvisualqa.report.HistoryEntry
import com.androidvisualqa.report.JsonReportWriter
import com.androidvisualqa.report.MarkdownReportWriter
import com.androidvisualqa.report.ReportAssembler
import com.androidvisualqa.report.ZipExporter
import com.androidvisualqa.model.serialization.JsonConfig
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.nio.file.Files
import java.util.UUID

/**
 * Result of a single capture operation.
 *
 * @property frame The captured frame metadata.
 * @property pngBytes The PNG-encoded screenshot bytes.
 */
public data class CaptureResult(
    val frame: CapturedFrame,
    val pngBytes: ByteArray,
    val candidates: List<NodeSnapshot> = emptyList(),
    val screenBounds: Bounds<CoordinateSpace.ScreenPx>? = null,
)

/**
 * Pure-logic orchestrator for the M2 capture -> draft -> matching -> report flow.
 *
 * Does NOT hold Android [Context] references. All Android dependencies
 * (accessibility service, pixel capture) are injected as parameters.
 *
 * @param clock Source of time; defaults to [Clock.System].
 */
public class CaptureOrchestrator(
    private val clock: Clock = Clock.System,
) {

    /**
     * Starts a capture from the active accessibility window.
     *
     * Uses the provided [captureFrame] lambda to obtain the capture metadata
     * and [packageName] lambda for the app package. This avoids coupling
     * directly to the [VisualFeedbackAccessibilityService] class so the
     * orchestrator is unit-testable.
     *
     * 1. Calls [captureFrame] to get the screenshot + frame metadata.
     * 2. Calls [packageName] to get the package name.
     * 3. Creates a draft and writes the original PNG.
     *
     * @param windowId The accessibility window ID to record in the [CaptureFrame].
     * @param captureFrame Suspending lambda that captures the active window
     *   and returns [CaptureResult] containing the frame metadata and PNG bytes.
     * @param packageName Suspending lambda that returns the package name.
     * @return [Result.success] with the new [DraftId], or [Result.failure] on error.
     */
    public suspend fun startCapture(
        windowId: Long,
        captureFrame: suspend () -> Result<CaptureResult>,
        packageName: suspend () -> String,
        draftStore: FileSystemDraftStore,
        reportHistory: FileSystemReportHistoryIndex,
        triggerSource: TriggerSource = TriggerSource.QuickSettingsTile,
    ): Result<DraftId> {
        // 1. Capture frame
        val captureResult = captureFrame().getOrElse { return Result.failure(it) }
        val pkgName = kotlin.runCatching { packageName() }.getOrDefault("unknown")

        // Build the frame metadata
        val frame = CaptureFrame(
            displayId = captureResult.frame.displayId,
            windowId = windowId.toInt(),
            packageName = pkgName,
            activityTitle = null,
            widthPx = captureResult.frame.widthPx,
            heightPx = captureResult.frame.heightPx,
            density = 0f, // TODO(m3): read from DisplayMetrics
            rotationDegrees = captureResult.frame.rotation.surfaceRotation,
            systemBarsTop = 0,
            systemBarsBottom = 0,
            systemBarsLeft = 0,
            systemBarsRight = 0,
            windowBoundsLeft = 0,
            windowBoundsTop = 0,
            windowBoundsRight = captureResult.frame.widthPx,
            windowBoundsBottom = captureResult.frame.heightPx,
            contentBoundsLeft = 0,
            contentBoundsTop = 0,
            contentBoundsRight = captureResult.frame.widthPx,
            contentBoundsBottom = captureResult.frame.heightPx,
            screenshotMethod = ScreenshotMethod.AccessibilityWindow,
            monotonicTimestamp = captureResult.frame.capturedAt.toEpochMilliseconds(),
            wallClockTimestamp = captureResult.frame.capturedAt,
        )

        // 2. Create a draft
        val draftId = draftStore.createDraft().getOrElse { return Result.failure(it) }

        // 3. Persist the original screenshot
        if (captureResult.pngBytes.isEmpty()) {
            return Result.failure(IllegalStateException("Capture returned no image data"))
        }
        draftStore.writeOriginal(draftId, captureResult.pngBytes).getOrElse { return Result.failure(it) }

        val createdAt = clock.now()
        val screenBounds = captureResult.screenBounds ?: Bounds(
            left = 0.0,
            top = 0.0,
            right = frame.widthPx.toDouble(),
            bottom = frame.heightPx.toDouble(),
            space = CoordinateSpace.ScreenPx,
        )

        // 6. Write a draft manifest
        draftStore.writeManifest(
            draftId,
            com.androidvisualqa.files.DraftManifest(
                draftId = draftId,
                createdAt = createdAt,
                reportSchemaVersion = com.androidvisualqa.model.VisualFeedbackReport.CURRENT_SCHEMA_VERSION,
                captureState = "Captured",
                displayId = frame.displayId,
                windowId = frame.windowId,
                packageName = frame.packageName,
                widthPx = frame.widthPx,
                heightPx = frame.heightPx,
                rotationDegrees = frame.rotationDegrees,
            ),
        ).getOrElse { return Result.failure(it) }

        val context = DraftCaptureContext(
            frame = frame,
            session = CaptureSession(
                sessionId = draftId.value,
                startedAt = createdAt,
                triggerSource = triggerSource,
                captureMode = CaptureMode.Still,
                lastStateName = "Captured",
                draftId = draftId,
            ),
            candidates = captureResult.candidates,
            screenLeft = screenBounds.left,
            screenTop = screenBounds.top,
            screenRight = screenBounds.right,
            screenBottom = screenBounds.bottom,
        )
        AtomicFileWriter.writeTextAtomically(
            draftStore.directory.captureContextPath(draftId),
            JsonConfig.encodeToString(context),
        ).getOrElse { return Result.failure(it) }

        return Result.success(draftId)
    }

    /**
     * Finishes a draft: runs matching, assembles the report, writes all output files.
     *
     * @param draftId The draft to finalise.
     * @param rectangle The rectangle annotation the user drew (bitmap-pixel coords).
     * @param feedback The user's feedback text.
     * @param candidates The accessibility node tree candidates to rank.
     * @param screenBounds Screen bounds for distance normalisation.
     * @param frame The [CaptureFrame] metadata.
     * @param session The [CaptureSession] captured.
     */
    public suspend fun finishDraft(
        draftId: DraftId,
        rectangle: RectangleAnnotation?,
        feedback: String,
        candidates: List<NodeSnapshot>,
        screenBounds: Bounds<CoordinateSpace.ScreenPx>,
        frame: CaptureFrame,
        session: CaptureSession,
        draftStore: FileSystemDraftStore,
        reportHistory: FileSystemReportHistoryIndex,
        draftDirectory: DraftDirectory,
    ): Result<VisualFeedbackReport> {
        // 1. Build the matching input from the user's rectangle
        val selectionAndAnnotation = rectangle?.let { rect ->
            val ranked = MatchingEngine().rank(
                MatchingInput(
                    selectionPolygon = rectangleToPolygon(rect, screenBounds, frame.widthPx, frame.heightPx),
                    screenBounds = screenBounds,
                    candidates = candidates,
                ),
            )
            val decision = DecisionPolicy.apply(ranked)
            ComponentSelection(
                selectionId = UUID.randomUUID().toString(),
                annotationId = rect.id.value,
                chosenNodeId = decision.top?.node?.nodeId,
                candidateNodeIds = decision.candidates.map { it.node.nodeId },
                confidence = decision.top?.confidence ?: 0.0,
                scoreOverlap = decision.top?.scoreOverlap ?: 0.0,
                scoreContainment = decision.top?.scoreContainment ?: 0.0,
                scoreCenterProximity = decision.top?.scoreCenterProximity ?: 0.0,
                scoreActionable = decision.top?.scoreActionable ?: 0.0,
                scoreSemanticRichness = decision.top?.scoreSemanticRichness ?: 0.0,
                scoreLeafPreference = decision.top?.scoreLeafPreference ?: 0.0,
                scoreRecentEvent = decision.top?.scoreRecentEvent ?: 0.0,
                scoreSdkEvidence = decision.top?.scoreSdkEvidence ?: 0.0,
                choiceType = decision.choiceType,
                evidenceSource = EvidenceSource.Accessibility,
            ) to AnnotationEvidence(
                annotationId = rect.id.value,
                toolType = AnnotationTool.Rectangle,
                boundingBoxLeft = normalised(rect.left, frame.widthPx),
                boundingBoxTop = normalised(rect.top, frame.heightPx),
                boundingBoxRight = normalised(rect.right, frame.widthPx),
                boundingBoxBottom = normalised(rect.bottom, frame.heightPx),
                displayRotationDegrees = frame.rotationDegrees,
                displayWidthPx = frame.widthPx,
                displayHeightPx = frame.heightPx,
            )
        }

        // 6. Build FeedbackEvidence
        val feedbackEvidence = FeedbackEvidence(textBody = feedback)

        // 7. Build PrivacyEvidence
        val privacyEvidence = PrivacyEvidence(
            secureWindowResult = SecureWindowResult.NotSecure,
        )

        // 8. Build AttachmentRef for the original screenshot
        val originalPngPath = draftDirectory.originalImagePath(draftId)
        val originalBytes = try {
            java.nio.file.Files.readAllBytes(originalPngPath)
        } catch (e: Exception) {
            ByteArray(0)
        }

        val attachmentRefs = listOf(
            AttachmentRef(
                attachmentId = AttachmentId(UUID.randomUUID().toString()),
                fileName = "original.png",
                mimeType = "image/png",
                sizeBytes = originalBytes.size.toLong(),
                sha256Hex = if (originalBytes.isNotEmpty()) Hashing.sha256(originalBytes) else "",
                role = "original_screenshot",
            ),
        )

        // 9. Assemble the report
        val assembler = ReportAssembler(clock)
        val assemblyInput = AssemblyInput(
            session = session,
            frame = frame,
            annotations = listOfNotNull(selectionAndAnnotation?.second),
            selections = listOfNotNull(selectionAndAnnotation?.first),
            feedback = feedbackEvidence,
            privacy = privacyEvidence,
            attachments = attachmentRefs,
        )
        val report = assembler.assemble(assemblyInput, ReportStatus.Saved)

        // 10. Write report files
        val jsonWriter = JsonReportWriter()
        val mdWriter = MarkdownReportWriter()
        val zipExporter = ZipExporter()

        jsonWriter.write(report, draftDirectory.reportJsonPath(draftId))
            .getOrElse { return Result.failure(it) }

        val mdContent = mdWriter.write(report)
        com.androidvisualqa.files.AtomicFileWriter.writeTextAtomically(
            draftDirectory.reportMarkdownPath(draftId),
            mdContent,
        ).getOrElse { return Result.failure(it) }

        // 11. Write updated manifest with completed state
        draftStore.writeManifest(
            draftId,
            com.androidvisualqa.files.DraftManifest(
                draftId = draftId,
                createdAt = clock.now(),
                originalSha256 = if (originalBytes.isNotEmpty()) Hashing.sha256(originalBytes) else null,
                reportSchemaVersion = com.androidvisualqa.model.VisualFeedbackReport.CURRENT_SCHEMA_VERSION,
                captureState = "Complete",
                displayId = frame.displayId,
                windowId = frame.windowId,
                packageName = frame.packageName,
                widthPx = frame.widthPx,
                heightPx = frame.heightPx,
                rotationDegrees = frame.rotationDegrees,
            ),
        ).getOrElse { return Result.failure(it) }

        // 12. Append to history
        reportHistory.append(
            HistoryEntry(
                draftId = draftId,
                reportId = report.reportId,
                createdAt = clock.now(),
                status = ReportStatus.Saved,
                packageName = frame.packageName,
            ),
        ).getOrElse { return Result.failure(it) }

        return Result.success(report)
    }

    /** Finishes a draft using the context persisted at capture time. */
    public suspend fun finishPersistedDraft(
        draftId: DraftId,
        rectangle: RectangleAnnotation?,
        feedback: String,
        draftStore: FileSystemDraftStore,
        reportHistory: FileSystemReportHistoryIndex,
        draftDirectory: DraftDirectory,
    ): Result<VisualFeedbackReport> {
        val context = readCaptureContext(draftId, draftDirectory).getOrElse { return Result.failure(it) }
        if (context != null) {
            return finishDraft(
                draftId = draftId,
                rectangle = rectangle,
                feedback = feedback,
                candidates = context.candidates,
                screenBounds = context.screenBounds(),
                frame = context.frame,
                session = context.session,
                draftStore = draftStore,
                reportHistory = reportHistory,
                draftDirectory = draftDirectory,
            )
        }

        val manifest = draftStore.readDraft(draftId).getOrElse { return Result.failure(it) }
            ?: return Result.failure(IllegalStateException("Draft ${draftId.value} has no manifest"))
        val now = clock.now()
        val frame = CaptureFrame(
            displayId = manifest.displayId,
            windowId = manifest.windowId,
            packageName = manifest.packageName,
            widthPx = manifest.widthPx,
            heightPx = manifest.heightPx,
            density = 0f,
            rotationDegrees = manifest.rotationDegrees,
            windowBoundsRight = manifest.widthPx,
            contentBoundsRight = manifest.widthPx,
            contentBoundsBottom = manifest.heightPx,
            screenshotMethod = ScreenshotMethod.AccessibilityWindow,
            monotonicTimestamp = now.toEpochMilliseconds(),
            wallClockTimestamp = now,
        )
        return finishDraft(
            draftId, rectangle, feedback, emptyList(),
            Bounds(0.0, 0.0, manifest.widthPx.toDouble(), manifest.heightPx.toDouble(), CoordinateSpace.ScreenPx),
            frame,
            CaptureSession(draftId.value, now, TriggerSource.ManualImport, CaptureMode.Still, draftId = draftId),
            draftStore, reportHistory, draftDirectory,
        )
    }

    public suspend fun readCaptureContext(
        draftId: DraftId,
        draftDirectory: DraftDirectory,
    ): Result<DraftCaptureContext?> = runCatching {
        val path = draftDirectory.captureContextPath(draftId)
        if (!Files.exists(path)) null else JsonConfig.decodeFromString<DraftCaptureContext>(path.toFile().readText())
    }

    // ─── Internal helpers ────────────────────────────────────────────────

    /**
     * Converts the editor's normalised rectangle to a [Polygon]
     * in [CoordinateSpace.ScreenPx].
     */
    private fun rectangleToPolygon(
        rect: RectangleAnnotation,
        screenBounds: Bounds<CoordinateSpace.ScreenPx>,
        bitmapWidth: Int,
        bitmapHeight: Int,
    ): Polygon<CoordinateSpace.ScreenPx> {
        val scaleX = screenBounds.width / bitmapWidth.toDouble()
        val scaleY = screenBounds.height / bitmapHeight.toDouble()
        val left = screenBounds.left + normalised(rect.left, bitmapWidth) * bitmapWidth * scaleX
        val top = screenBounds.top + normalised(rect.top, bitmapHeight) * bitmapHeight * scaleY
        val right = screenBounds.left + normalised(rect.right, bitmapWidth) * bitmapWidth * scaleX
        val bottom = screenBounds.top + normalised(rect.bottom, bitmapHeight) * bitmapHeight * scaleY
        return Polygon(
            listOf(
                Point(left, top, CoordinateSpace.ScreenPx),
                Point(right, top, CoordinateSpace.ScreenPx),
                Point(right, bottom, CoordinateSpace.ScreenPx),
                Point(left, bottom, CoordinateSpace.ScreenPx),
            ),
        )
    }

    private fun normalised(value: Float, size: Int): Double =
        if (value in 0f..1f) value.toDouble() else value.toDouble() / size.coerceAtLeast(1)

    public companion object {
        /** Creates a [CaptureOrchestrator] with default dependencies. */
        public fun create(): CaptureOrchestrator = CaptureOrchestrator()
    }
}
