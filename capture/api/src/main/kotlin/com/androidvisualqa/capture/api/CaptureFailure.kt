package com.androidvisualqa.capture.api

/**
 * Reasons a capture attempt can fail before reaching the editor.
 *
 * @see CaptureState.Failed
 */
public sealed interface CaptureFailure {
    /** The app does not hold the required permission. */
    public data object PermissionDenied : CaptureFailure

    /** The [android.accessibilityservice.AccessibilityService] disconnected. */
    public data object ServiceDisconnected : CaptureFailure

    /** The target window is no longer available. */
    public data object WindowUnavailable : CaptureFailure

    /** The screenshot source returned no data. */
    public data object ScreenshotUnavailable : CaptureFailure

    /** The user declined the MediaProjection consent dialog. */
    public data object MediaProjectionConsentCancelled : CaptureFailure

    /** An I/O or storage-level error. */
    public data class StorageFailure(val cause: String) : CaptureFailure

    /** An unrecoverable or unmatched error. */
    public data class Unknown(val cause: String) : CaptureFailure
}
