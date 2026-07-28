package com.androidvisualqa.pixels

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PixelCaptureRequestTest {

    @Test
    fun `default timeout applied`() {
        val request = PixelCaptureRequest(displayId = 0)
        assertEquals(5_000L, request.timeoutMs)
    }

    @Test
    fun `windowId defaults to null`() {
        val request = PixelCaptureRequest(displayId = 0)
        assertNull(request.windowId)
    }

    @Test
    fun `explicit values are preserved`() {
        val request = PixelCaptureRequest(displayId = 1, windowId = 42L, timeoutMs = 10_000)
        assertEquals(1, request.displayId)
        assertEquals(42L, request.windowId)
        assertEquals(10_000L, request.timeoutMs)
    }
}
