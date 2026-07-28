package com.androidvisualqa.accessibility

import android.accessibilityservice.AccessibilityService

/**
 * Snapshot of the currently focused accessibility window.
 *
 * @property packageName The package that owns the active window.
 * @property windowId The accessibility window ID of the active window.
 */
public data class ActiveWindowInfo(
    val packageName: String?,
    val windowId: Long?,
)

/**
 * Captures metadata about the *currently focused* window from the service's
 * [AccessibilityService.windows] list.
 *
 * Used by the trigger flow to determine which window to capture before a
 * full tree snapshot is taken.
 */
public object ActiveWindowSnapshotter {

    /**
     * Returns the package name and window ID of the currently focused window.
     *
     * Iterates [service.windows] looking for the window with the highest
     * layer (z-order). If no windows are available, returns an empty result.
     */
    public fun snapshot(service: AccessibilityService): ActiveWindowInfo {
        val windows = service.windows ?: return ActiveWindowInfo(null, null)

        var topPackage: String? = null
        var topWindowId: Long? = null
        var topLayer = Int.MIN_VALUE

        for (window in windows) {
            if (window.isActive && window.layer > topLayer) {
                topPackage = window.root?.let { root ->
                    try {
                        root.packageName?.toString()
                    } finally {
                        root.recycle()
                    }
                }
                topWindowId = window.id.toLong()
                topLayer = window.layer
            }
        }

        // Second pass: if no window was explicitly active, take the highest layer
        if (topWindowId == null) {
            for (window in windows) {
                if (window.layer > topLayer) {
                    topPackage = window.root?.let { root ->
                        try {
                            root.packageName?.toString()
                        } finally {
                            root.recycle()
                        }
                    }
                    topWindowId = window.id.toLong()
                    topLayer = window.layer
                }
            }
        }

        return ActiveWindowInfo(topPackage, topWindowId)
    }
}
