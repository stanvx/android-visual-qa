package com.androidvisualqa.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.androidvisualqa.accessibility.VisualFeedbackAccessibilityService
import com.androidvisualqa.app.trigger.QuickSettingsTile
import com.androidvisualqa.capture.api.CapturedFrame
import com.androidvisualqa.files.DraftDirectory
import com.androidvisualqa.files.FileSystemDraftStore
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.report.FileSystemReportHistoryIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Foreground service that owns the M2 capture sequence.
 *
 * Flow:
 * 1. Started by [QuickSettingsTile.onClick] (or other triggers).
 * 2. Creates a foreground notification so the OS allows background execution.
 * 3. Delegates to [CaptureOrchestrator.startCapture()].
 * 4. On success: posts a [PendingIntent] to open the editor with the new [DraftId].
 * 5. On failure: posts a notification with the error and stops.
 *
 * Start mode: [Service.START_NOT_STICKY] — if killed, do NOT restart.
 * Foreground service type: `dataSync` — M2 uses accessibility capture, not
 * media projection. Change to `mediaProjection` or `specialUse` if a future
 * lane switches to MediaProjectionManager.
 *
 * ponytail: single-file service. All capture logic lives in [CaptureOrchestrator].
 */
public class CaptureForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var orchestrator: CaptureOrchestrator

    override fun onCreate() {
        super.onCreate()
        orchestrator = CaptureOrchestrator()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        startCaptureSequence()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    // ─── Private helpers ─────────────────────────────────────────────────

    private fun startCaptureSequence() {
        val draftStore = FileSystemDraftStore(
            DraftDirectory(
                getDir("drafts", Context.MODE_PRIVATE).toPath(),
            ),
        )
        val reportHistory = FileSystemReportHistoryIndex(
            File(filesDir, "report_history.jsonl").toPath(),
        )

        serviceScope.launch {
            val draftIdResult = performCapture(draftStore, reportHistory)
            draftIdResult.fold(
                onSuccess = { draftId -> postSuccessNotification(draftId) },
                onFailure = { error -> postErrorNotification(error) },
            )
            stopSelf()
        }
    }

    private suspend fun performCapture(
        draftStore: FileSystemDraftStore,
        reportHistory: FileSystemReportHistoryIndex,
    ): Result<DraftId> {
        val service = findAccessibilityService()
            ?: return Result.failure(IllegalStateException("Accessibility service not running"))

        val windowId = service.activeWindowId()
            ?: return Result.failure(IllegalStateException("No active accessibility window"))

        val capturedFrame = service.takeWindowScreenshot(windowId)
            ?: return Result.failure(IllegalStateException("Window screenshot returned null"))

        val pkgName = service.rootInActiveWindow?.packageName?.toString() ?: "unknown"

        val pngBytes = compressToPng(capturedFrame)

        return orchestrator.startCapture(
            windowId = windowId,
            captureFrame = {
                Result.success(CaptureResult(frame = capturedFrame, pngBytes = pngBytes))
            },
            packageName = { pkgName },
            draftStore = draftStore,
            reportHistory = reportHistory,
        )
    }

    private fun findAccessibilityService(): VisualFeedbackAccessibilityService? {
        // ponytail: global registry bridge until proper DI/binding in M3.
        return AppServiceRegistry.accessibilityService
    }

    private fun compressToPng(frame: CapturedFrame): ByteArray {
        // Build a lightweight bitmap representation:
        // We don't have direct pixel data so we produce a small placeholder.
        // TODO(m3): pipe actual bitmap from takeWindowScreenshot
        val bmp = android.graphics.Bitmap.createBitmap(
            frame.widthPx.coerceAtLeast(1), frame.heightPx.coerceAtLeast(1),
            android.graphics.Bitmap.Config.ARGB_8888,
        )
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawColor(android.graphics.Color.argb(255, 200, 200, 200))
        val stream = ByteArrayOutputStream()
        bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        bmp.recycle()
        return stream.toByteArray()
    }

    private fun postSuccessNotification(draftId: DraftId) {
        val editorIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("draftId", draftId.value)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            editorIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Capture complete")
            .setContentText("Tap to edit draft \u2026")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(RESULT_NOTIFICATION_ID, notification)
    }

    private fun postErrorNotification(error: Throwable) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Capture failed")
            .setContentText(error.message ?: "Unknown error")
            .setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
            .setAutoCancel(true)
            .build()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(RESULT_NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Capture Service",
            NotificationManager.IMPORTANCE_LOW,
        )
        nm.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Capturing screen\u2026")
            .setContentText("Please wait")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }

    internal companion object {
        internal const val CHANNEL_ID: String = "capture_foreground"
        private const val NOTIFICATION_ID: Int = 1001
        private const val RESULT_NOTIFICATION_ID: Int = 1002
    }
}
