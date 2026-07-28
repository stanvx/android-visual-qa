package com.androidvisualqa.export.agent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BoundsNormalizerTest {

    @Test
    fun `exact bounds at canvas edges produce (0,0,1,1)`() {
        val b = BoundsNormalizer.normalize(
            left = 0, top = 0, right = 1080, bottom = 1920,
            canvasWidth = 1080, canvasHeight = 1920,
        )
        assertEquals(0.0, b.left, 1e-9)
        assertEquals(0.0, b.top, 1e-9)
        assertEquals(1.0, b.right, 1e-9)
        assertEquals(1.0, b.bottom, 1e-9)
    }

    @Test
    fun `out-of-bounds coordinates are clamped to 0_0_1_1`() {
        val b = BoundsNormalizer.normalize(
            left = -100, top = -200, right = 2000, bottom = 3000,
            canvasWidth = 1080, canvasHeight = 1920,
        )
        assertEquals(0.0, b.left, 1e-9)
        assertEquals(0.0, b.top, 1e-9)
        assertEquals(1.0, b.right, 1e-9)
        assertEquals(1.0, b.bottom, 1e-9)
    }

    @Test
    fun `mid-canvas bounds normalise correctly`() {
        val b = BoundsNormalizer.normalize(
            left = 540, top = 960, right = 810, bottom = 1440,
            canvasWidth = 1080, canvasHeight = 1920,
        )
        assertEquals(0.5, b.left, 1e-9)
        assertEquals(0.5, b.top, 1e-9)
        assertEquals(0.75, b.right, 1e-9)
        assertEquals(0.75, b.bottom, 1e-9)
    }

    @Test
    fun `double overload matches int overload`() {
        val intResult = BoundsNormalizer.normalize(100, 200, 300, 400, 800, 600)
        val doubleResult = BoundsNormalizer.normalize(100.0, 200.0, 300.0, 400.0, 800.0, 600.0)
        assertEquals(intResult, doubleResult)
    }

    @Test
    fun `zero canvas dimensions throw`() {
        assertThrows<IllegalArgumentException> {
            BoundsNormalizer.normalize(0, 0, 100, 100, 0, 100)
        }
        assertThrows<IllegalArgumentException> {
            BoundsNormalizer.normalize(0, 0, 100, 100, 100, 0)
        }
    }

    @Test
    fun `values already in range are unchanged`() {
        val b = BoundsNormalizer.normalize(
            left = 0.3, top = 0.4, right = 0.7, bottom = 0.9,
            canvasWidth = 1.0, canvasHeight = 1.0,
        )
        assertEquals(0.3, b.left, 1e-9)
        assertEquals(0.4, b.top, 1e-9)
        assertEquals(0.7, b.right, 1e-9)
        assertEquals(0.9, b.bottom, 1e-9)
    }

    @Test
    fun `negative values below canvas clamp to zero`() {
        val b = BoundsNormalizer.normalize(
            left = -0.5, top = -1.0, right = 0.5, bottom = 0.5,
            canvasWidth = 1.0, canvasHeight = 1.0,
        )
        assertTrue(b.left >= 0.0)
        assertTrue(b.top >= 0.0)
        assertEquals(0.5, b.right, 1e-9)
        assertEquals(0.5, b.bottom, 1e-9)
    }
}
