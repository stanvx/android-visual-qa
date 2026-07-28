package com.androidvisualqa.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Resolves an accessibility window's root node from a window ID.
 *
 * Uses [AccessibilityService.getWindows] when available (API 21+), falling
 * back to [AccessibilityService.rootInActiveWindow]. The former is preferred
 * because it allows selecting a specific window by ID.
 */
public object WindowResolver {

    /**
     * Resolves the root [AccessibilityNodeInfo] for [windowId].
     *
     * @param service The connected accessibility service.
     * @param windowId The target accessibility window ID.
     * @return The root node, or `null` if the window is not found or the
     *         service is not connected.
     */
    public fun resolve(service: AccessibilityService, windowId: Long): AccessibilityNodeInfo? {
        // Prefer per-window resolution via getWindows()
        val windows = service.windows
        if (windows != null) {
            for (windowInfo in windows) {
                if (windowInfo.id.toLong() == windowId) {
                    val root = windowInfo.root
                    if (root != null) return root
                }
            }
        }

        // Fallback: use rootInActiveWindow if its windowId matches.
        val activeRoot = service.rootInActiveWindow
        if (activeRoot != null) {
            if (activeRoot.windowId.toLong() == windowId) {
                return activeRoot
            }
            activeRoot.recycle()
        }

        return null
    }
}
