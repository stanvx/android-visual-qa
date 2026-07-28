package com.androidvisualqa.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.androidvisualqa.accessibility.VisualFeedbackAccessibilityService
import com.androidvisualqa.accessibility.AccessibilityCaptureModule
import com.androidvisualqa.app.trigger.QuickSettingsTile
import com.androidvisualqa.files.DraftDirectory
import com.androidvisualqa.files.FileSystemDraftStore
import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.report.FileSystemReportHistoryIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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

        val activeRoot = service.rootInActiveWindow
        val windowId = service.activeWindowId()
            ?: activeRoot?.windowId?.toLong()
            ?: return Result.failure(IllegalStateException("No active accessibility window"))

        if (windowId == 0L) {
            return Result.failure(IllegalStateException("No active app window (home/recents?)"))
        }

        val capturedFrame = service.takeWindowScreenshot(windowId)
            ?: return Result.failure(IllegalStateException("Window screenshot returned null"))
        if (capturedFrame.pngBytes.isEmpty()) {
            return Result.failure(IllegalStateException("Window screenshot contained no pixels"))
        }

        val pkgName = activeRoot?.packageName?.toString() ?: "unknown"
        activeRoot?.recycle()
        val tree = AccessibilityCaptureModule { service }.snapshotTree(windowId)
        val screenBounds = service.windows
            ?.firstOrNull { it.id.toLong() == windowId }
            ?.let { window ->
                val bounds = Rect()
                window.getBoundsInScreen(bounds)
                Bounds(
                    left = bounds.left.toDouble(),
                    top = bounds.top.toDouble(),
                    right = bounds.right.toDouble(),
                    bottom = bounds.bottom.toDouble(),
                    space = CoordinateSpace.ScreenPx,
                )
            }

        return orchestrator.startCapture(
            windowId = windowId,
            captureFrame = {
                Result.success(
                    CaptureResult(
                        frame = capturedFrame,
                        pngBytes = capturedFrame.pngBytes,
                        candidates = tree.nodes,
                        screenBounds = screenBounds,
                    ),
                )
            },
            packageName = { pkgName },
            draftStore = draftStore,
            reportHistory = reportHistory,
        )
    }

    private suspend fun findAccessibilityService(): VisualFeedbackAccessibilityService? {
        // ponytail: registry may not be set yet if the foreground service starts
        // before the bridge's onServiceConnected() callback lands. Retry for up
        // to 2 s so the QS-tap path works on cold start.
        repeat(20) {
            AppServiceRegistry.accessibilityService?.let { return it }
            kotlinx.coroutines.delay(100)
        }
        return AppServiceRegistry.accessibilityService
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
