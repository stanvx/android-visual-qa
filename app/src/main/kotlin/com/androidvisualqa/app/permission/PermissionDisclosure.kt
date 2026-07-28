package com.androidvisualqa.app.permission

/**
 * Public-facing rationale for each permission requested by the app.
 *
 * These explanations are shown on the disclosure screen on first launch and
 * are available from Settings. They are written for a non-technical audience.
 *
 * ## Accessibility Service
 *
 * Visual QA uses the Accessibility Service to read the structure of the
 * currently visible screen. This is the only way to identify which UI element
 * a user is pointing at when they create a visual feedback report.
 *
 * What we read:
 * - The layout hierarchy (which views are on screen, their positions, and
 *   accessibility labels).
 * - Text content of non-secured elements (we never read password fields or
 *   other `isPassword`-flagged inputs).
 *
 * What we do NOT do:
 * - Log keystrokes or track navigation history.
 * - Send any data off the device without an explicit export action.
 * - Persist accessibility tree data beyond the current draft lifecycle.
 *
 * ## Notification Permission (Android 13+)
 *
 * Notification permission is requested only on Android 13 (API 33) and above.
 * The app uses a single ongoing notification while a capture is in progress
 * and posts a brief result notification when the capture completes. You can
 * revoke this permission at any time in system settings; capture results will
 * still be available in the draft list.
 *
 * ## Local Storage
 *
 * Screenshots, annotations, and report files are written to the app's private
 * internal storage. Other applications cannot access this data. When you
 * delete a draft from the draft list, all associated files are removed.
 *
 * Exported files (via Share ZIP or Save to Downloads) are written to
 * locations you explicitly choose — the system share sheet or the public
 * Downloads directory — and leave app-private storage at that point.
 *
 * ## Network Access
 *
 * The app does not have any background network access. No telemetry, no
 * analytics, no crash reporting. Network is only used when you trigger an
 * export action (Share ZIP) and the system share sheet delivers the file to
 * another app. The share destination is your choice; Visual QA does not
 * upload data to any server.
 *
 * ## Why these permissions together?
 *
 * The Accessibility Service provides the screen structure needed to produce
 * accurate reports. Notifications keep you informed of capture progress.
 * Storage holds your work until you choose to export or delete it. These
 * permissions work in concert to keep all processing on-device and under
 * your control. No data leaves the device unless you explicitly share it.
 */
public object PermissionDisclosure {
    /** Ordered list of section headings for rendering. */
    public val sections: List<String> = listOf(
        "Accessibility Service",
        "Notification Permission (Android 13+)",
        "Local Storage",
        "Network Access",
    )
}
