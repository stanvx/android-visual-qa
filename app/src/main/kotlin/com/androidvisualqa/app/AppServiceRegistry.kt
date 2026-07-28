package com.androidvisualqa.app

import com.androidvisualqa.accessibility.VisualFeedbackAccessibilityService

/**
 * Global registry for key services that the `:app` module needs to
 * coordinate between.
 *
 * ponytail: static global instead of DI framework. Replace with
 * Hilt or manual DI in M3 if the app gains more services.
 *
 * // TODO(m3): replace with proper dependency injection
 */
internal object AppServiceRegistry {

    /**
     * The running [VisualFeedbackAccessibilityService] instance, or `null`.
     *
     * Set by the service's [VisualFeedbackAccessibilityService.onServiceConnected]
     * via a companion-object extension. This is a temporary M2 bridge until
     * a proper binding mechanism (IPC or DI) is in place.
     */
    @Volatile
    public var accessibilityService: VisualFeedbackAccessibilityService? = null
}
