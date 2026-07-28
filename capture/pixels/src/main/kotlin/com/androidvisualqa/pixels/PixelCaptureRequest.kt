package com.androidvisualqa.pixels

/**
 * Request parameters for a pixel capture operation.
 *
 * @property displayId The logical Android display ID to capture from.
 * @property windowId The accessibility window ID to capture, or null for full display capture.
 * @property timeoutMs Maximum time to wait for the capture operation (milliseconds).
 */
public data class PixelCaptureRequest(
    val displayId: Int,
    val windowId: Long? = null,
    val timeoutMs: Long = 5_000,
)
