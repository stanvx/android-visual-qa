package com.androidvisualqa.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility service that captures UI state when triggered.
 *
 * // M2: implement CaptureCommand.Trigger dispatch on onServiceConnected().
 * // Events are filtered per accessibility_service_config.xml.
 */
public class VisualFeedbackAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        // M2: register a callback that dispatches CaptureCommand.Trigger
        // when the user long-presses the status bar, or when the Quick Settings
        // tile's capture pill is acknowledged.
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // M2: forward relevant events to the capture state machine
    }

    override fun onInterrupt() {
        // M2: clean up resources
    }
}
