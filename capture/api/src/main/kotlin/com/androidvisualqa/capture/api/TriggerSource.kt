package com.androidvisualqa.capture.api

/**
 * Identifies how the user initiated a capture session.
 *
 * This is the capture-layer trigger classification, distinct from the
 * persisted [com.androidvisualqa.model.capture.TriggerSource] which
 * reflects the schema-level source after the session is created.
 */
public enum class TriggerSource {
    AccessibilityBubble,
    QuickSettingsTile,
    NotificationAction,
    HardwareShortcut,
}
