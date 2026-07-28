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
    override fun onServiceConnected() {
        super.onServiceConnected()
        AppServiceRegistry.accessibilityService = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (AppServiceRegistry.accessibilityService === this) {
            AppServiceRegistry.accessibilityService = null
        }
    }
}
