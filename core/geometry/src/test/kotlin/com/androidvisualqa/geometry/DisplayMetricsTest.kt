package com.androidvisualqa.geometry

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DisplayMetricsTest {

    @Test
    fun `density is stored and returned`() {
        val dm = DisplayMetrics(density = 2.0, widthPx = 1080, heightPx = 1920, rotation = 0)
        assertEquals(2.0, dm.density, 1e-9)
    }

    @Test
    fun `dimensions are stored`() {
        val dm = DisplayMetrics(density = 3.0, widthPx = 1440, heightPx = 3120, rotation = 0)
        assertEquals(1440, dm.widthPx)
        assertEquals(3120, dm.heightPx)
    }

    @Test
    fun `rotation maps correctly`() {
        assertEquals(Rotation.ROTATION_0, DisplayMetrics(density = 1.0, widthPx = 100, heightPx = 100, rotation = 0).rotationEnum)
        assertEquals(Rotation.ROTATION_90, DisplayMetrics(density = 1.0, widthPx = 100, heightPx = 100, rotation = 1).rotationEnum)
        assertEquals(Rotation.ROTATION_180, DisplayMetrics(density = 1.0, widthPx = 100, heightPx = 100, rotation = 2).rotationEnum)
        assertEquals(Rotation.ROTATION_270, DisplayMetrics(density = 1.0, widthPx = 100, heightPx = 100, rotation = 3).rotationEnum)
    }

    @Test
    fun `unknown rotation defaults to zero`() {
        assertEquals(Rotation.ROTATION_0, DisplayMetrics(density = 1.0, widthPx = 100, heightPx = 100, rotation = 99).rotationEnum)
    }
}
