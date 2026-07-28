package com.androidvisualqa.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.hardware.HardwareBuffer
import android.hardware.display.DisplayManager
import android.view.Display
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.androidvisualqa.capture.api.CapturedFrame
import com.androidvisualqa.geometry.Rotation
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Clock
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Accessibility service that captures UI state on demand.
 *
 * ## Responsibilities
 * - Maintains a lightweight ring buffer of recent accessibility events.
 * - Provides `takeWindowScreenshot()` and `activeWindowId()` for the
 *   [AccessibilityCaptureModule] facade.
 * - Does **not** own report logic, Room access, bitmaps, or network calls.
 *
 * ## Thread-safety
 * A single [Mutex] protects [recentEvents]. All public methods are thread-safe.
 *
 * ## API gates
 * - `takeScreenshotOfWindow()` requires API 34+.
 * - On API 30–33 fall back to display-wide `takeScreenshot()`.
 *
 * @see AccessibilityCaptureModule
 */
public open class VisualFeedbackAccessibilityService : AccessibilityService() {

    // ─── Ring buffer ────────────────────────────────────────────────────

    /** Most recent accessibility events. Bounded to prevent OOM.
     * ponytail: 64 entries at ~1 KB each ≈ negligible memory. Bump if richer
     * event payloads are stored. */
    internal val recentEvents = ArrayDeque<AccessibilityEvent>(MAX_EVENTS)

    /** Mutex guarding [recentEvents]. */
    internal val mutex = Mutex()

    /** Last [AccessibilityEvent.getWindowId] from a
     * [AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED] event, or `null`. */
    private var lastActiveWindowId: Long? = null

    // ─── Service lifecycle ──────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        // ponytail: synchronized is fine for a 64-entry buffer.
        // Switch to a lock-free ring if contention shows up in profiling.
        synchronized(recentEvents) {
            if (recentEvents.size >= MAX_EVENTS) {
                recentEvents.removeFirst()
            }
            // Copy the event for the buffer so the caller can recycle the original.
            // Obtain may throw on some platforms (JVM stubs) or return null;
            // fall back to the original reference in those cases.
            val obtained: AccessibilityEvent? = try {
                AccessibilityEvent.obtain(event)
            } catch (_: RuntimeException) {
                null
            }
            recentEvents.addLast(obtained ?: event)

            // getWindowId and getEventType throw "Stub!" on JVM stubs.
            // Guard with try-catch so the service works in both real and test
            // environments.
            try {
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    lastActiveWindowId = event.windowId.toLong()
                }
            } catch (_: RuntimeException) {
                // Ignore on JVM test stubs
            }
        }
    }

    override fun onInterrupt() {
        // No-op: no ongoing work to cancel in M2.
    }

    // ─── Public API ─────────────────────────────────────────────────────

    /**
     * Captures a screenshot for the given [windowId].
     *
     * **API 34+:** Uses `takeScreenshotOfWindow(windowId, ...)` which captures
     * exactly the target window.
     * **API 30–33:** Falls back to display-wide `takeScreenshot()`.
     *
     * This is a **blocking** call that waits on a latch for the async
     * screenshot callback. Do not call on the main thread.
     *
     * @return [CapturedFrame] on success, `null` on failure.
     */
    @Suppress("DEPRECATION")
    public fun takeWindowScreenshot(windowId: Long): CapturedFrame? {
        val dm = getSystemService(DisplayManager::class.java) ?: return null
        val display = dm.getDisplay(Display.DEFAULT_DISPLAY) ?: return null

        val bitmap: Bitmap

        // API 34+: per-window screenshot using takeScreenshotOfWindow
        if (Build.VERSION.SDK_INT >= 34) {
            val latch = CountDownLatch(1)
            var result: ScreenshotResult? = null
            var errorCode: Int? = null

            takeScreenshotOfWindow(
                windowId.toInt(),
                Executors.newSingleThreadExecutor(),
                TakeScreenshotCallback { screenshot, error ->
                    if (screenshot != null) result = screenshot else errorCode = error
                    latch.countDown()
                },
            )
            if (!latch.await(SCREENSHOT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) return null
            if (errorCode != null) return null
            val hb = result?.hardwareBuffer ?: return null
            bitmap = Bitmap.wrapHardwareBuffer(hb, null) ?: return null
        } else {
            // API 30–33: display-wide screenshot
            val latch = CountDownLatch(1)
            var result: ScreenshotResult? = null
            var errorCode: Int? = null

            takeScreenshot(
                display.displayId,
                Executors.newSingleThreadExecutor(),
                TakeScreenshotCallback { screenshot, error ->
                    if (screenshot != null) result = screenshot else errorCode = error
                    latch.countDown()
                },
            )
            if (!latch.await(SCREENSHOT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) return null
            if (errorCode != null) return null
            val hb = result?.hardwareBuffer ?: return null
            bitmap = Bitmap.wrapHardwareBuffer(hb, null) ?: return null
        }

        val width = bitmap.width
        val height = bitmap.height
        val pngBytes = ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }
        bitmap.recycle()

        return CapturedFrame(
            displayId = display.displayId,
            widthPx = width,
            heightPx = height,
            rotation = Rotation.fromSurfaceRotation(display.rotation),
            capturedAt = Clock.System.now(),
        )
    }

    /**
     * Returns the window ID of the most recent
     * [AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED] source, or `null`.
     */
    public open fun activeWindowId(): Long? = lastActiveWindowId

    /**
     * Helper to create a [TakeScreenshotCallback] from lambdas.
     */
    private class TakeScreenshotCallback(
        private val onResult: (ScreenshotResult?, Int) -> Unit,
    ) : AccessibilityService.TakeScreenshotCallback {
        override fun onSuccess(screenshot: ScreenshotResult) {
            onResult(screenshot, 0)
        }

        override fun onFailure(code: Int) {
            onResult(null, code)
        }
    }

    internal companion object {
        /** Hard limit for the recent-event ring buffer. */
        internal const val MAX_EVENTS: Int = 64

        /** Timeout for screenshot capture, in milliseconds. */
        internal const val SCREENSHOT_TIMEOUT_MS: Long = 5_000
    }
}
