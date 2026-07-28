package com.androidvisualqa.pixels

import com.androidvisualqa.geometry.Rotation
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class CapturedPixelsTest {

    private val now = Instant.parse("2026-07-28T10:00:00Z")

    @Test
    fun `same data is equal`() {
        val a = CapturedPixels(
            displayId = 0,
            widthPx = 100,
            heightPx = 200,
            rotation = Rotation.ROTATION_0,
            rgba8888 = byteArrayOf(1, 2, 3, 4),
            capturedAt = now,
        )
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `different byte arrays are not equal`() {
        val a = CapturedPixels(
            displayId = 0,
            widthPx = 100,
            heightPx = 200,
            rotation = Rotation.ROTATION_0,
            rgba8888 = byteArrayOf(1, 2, 3, 4),
            capturedAt = now,
        )
        val b = a.copy(rgba8888 = byteArrayOf(5, 6, 7, 8))
        assertNotEquals(a, b)
    }

    @Test
    fun `contentEquals used for byte array`() {
        val a = CapturedPixels(
            displayId = 0,
            widthPx = 100,
            heightPx = 200,
            rotation = Rotation.ROTATION_0,
            rgba8888 = byteArrayOf(1, 2, 3, 4),
            capturedAt = now,
        )
        // Same bytes, different array instance
        val b = a.copy(rgba8888 = byteArrayOf(1, 2, 3, 4))
        assertEquals(a, b)
    }
}
