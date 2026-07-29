package com.androidvisualqa.app

import com.androidvisualqa.accessibility.VisualFeedbackAccessibilityService

/**
 * M2 bridge that registers the running accessibility service instance into
 * [AppServiceRegistry] so [CaptureForegroundService] can discover it.
 *
 * The manifest declares [VisualFeedbackAccessibilityService] directly; this
 * class is only used if the manifest is pointed at it instead. For M2 the
 * bridge registers the instance in [onServiceConnected].
 *
 * ponytail: subclass + global registry instead of proper IPC. Replace in M3.
 * // TODO(m3): remove this bridge; replace with DI or proper binding.
 */
internal class BridgeAccessibilityService : VisualFeedbackAccessibilityService() {

    override fun onCreate() {
        super.onCreate()
        // Register in onCreate (always called) rather than onServiceConnected
        // (which Android 14+ may defer until the first accessibility event lands).
        // The CaptureForegroundService polls for up to 2s, so as long as we're
        // registered before the user taps the QS tile, capture works.
        AppServiceRegistry.accessibilityService = this
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Also update here in case the service instance is reused.
        AppServiceRegistry.accessibilityService = this
        CaptureForegroundService.showReadyNotification(this)
    }

    override fun onDestroy() {
        CaptureForegroundService.cancelReadyNotification(this)
        super.onDestroy()
        if (AppServiceRegistry.accessibilityService === this) {
            AppServiceRegistry.accessibilityService = null
        }
    }
}
