package com.androidvisualqa.geometry

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class BoundsTest {

    // ------------------------------------------------------------------
    // isEmpty
    // ------------------------------------------------------------------

    @Test
    fun `empty when width is zero`() {
        val b = Bounds(0.0, 0.0, 0.0, 10.0, CoordinateSpace.ScreenPx)
        assertTrue(b.isEmpty)
    }

    @Test
    fun `empty when height is zero`() {
        val b = Bounds(0.0, 0.0, 10.0, 0.0, CoordinateSpace.ScreenPx)
        assertTrue(b.isEmpty)
    }

    @Test
    fun `not empty for positive area`() {
        val b = Bounds(0.0, 0.0, 10.0, 10.0, CoordinateSpace.ScreenPx)
        assertFalse(b.isEmpty)
    }

    // ------------------------------------------------------------------
    // intersection
    // ------------------------------------------------------------------

    @Test
    fun `intersection of disjoint bounds is empty`() {
        val a = Bounds(0.0, 0.0, 10.0, 10.0, CoordinateSpace.ScreenPx)
        val b = Bounds(20.0, 20.0, 30.0, 30.0, CoordinateSpace.ScreenPx)
        val result = a.intersection(b)
        assertTrue(result.isEmpty)
    }

    @Test
    fun `intersection of overlapping bounds is correct`() {
        val a = Bounds(0.0, 0.0, 10.0, 10.0, CoordinateSpace.ScreenPx)
        val b = Bounds(5.0, 5.0, 15.0, 15.0, CoordinateSpace.ScreenPx)
        val result = a.intersection(b)
        assertEquals(5.0, result.left)
        assertEquals(5.0, result.top)
        assertEquals(10.0, result.right)
        assertEquals(10.0, result.bottom)
    }

    @Test
    fun `intersection with fully contained inner rect`() {
        val a = Bounds(0.0, 0.0, 100.0, 100.0, CoordinateSpace.ScreenPx)
        val b = Bounds(10.0, 10.0, 20.0, 20.0, CoordinateSpace.ScreenPx)
        val result = a.intersection(b)
        assertEquals(10.0, result.left)
        assertEquals(10.0, result.top)
        assertEquals(20.0, result.right)
        assertEquals(20.0, result.bottom)
    }

    // ------------------------------------------------------------------
    // contains
    // ------------------------------------------------------------------

    @Test
    fun `contains point inside`() {
        val b = Bounds(0.0, 0.0, 10.0, 10.0, CoordinateSpace.ScreenPx)
        assertTrue(b.contains(screenPx(5.0, 5.0)))
    }

    @Test
    fun `contains point outside`() {
        val b = Bounds(0.0, 0.0, 10.0, 10.0, CoordinateSpace.ScreenPx)
        assertFalse(b.contains(screenPx(15.0, 5.0)))
    }

    @Test
    fun `contains point on left edge`() {
        val b = Bounds(0.0, 0.0, 10.0, 10.0, CoordinateSpace.ScreenPx)
        assertTrue(b.contains(screenPx(0.0, 5.0)))
    }

    @Test
    fun `contains point on right edge`() {
        val b = Bounds(0.0, 0.0, 10.0, 10.0, CoordinateSpace.ScreenPx)
        assertTrue(b.contains(screenPx(10.0, 5.0)))
    }

    @Test
    fun `contains point on top edge`() {
        val b = Bounds(0.0, 0.0, 10.0, 10.0, CoordinateSpace.ScreenPx)
        assertTrue(b.contains(screenPx(5.0, 0.0)))
    }

    @Test
    fun `contains point on bottom edge`() {
        val b = Bounds(0.0, 0.0, 10.0, 10.0, CoordinateSpace.ScreenPx)
        assertTrue(b.contains(screenPx(5.0, 10.0)))
    }

    // ------------------------------------------------------------------
    // center
    // ------------------------------------------------------------------

    @Test
    fun `center of unit square`() {
        val b = Bounds(0.0, 0.0, 1.0, 1.0, CoordinateSpace.ScreenPx)
        val c = b.center
        assertEquals(0.5, c.x)
        assertEquals(0.5, c.y)
    }

    @Test
    fun `center of non-origin rectangle`() {
        val b = Bounds(10.0, 20.0, 30.0, 40.0, CoordinateSpace.ScreenPx)
        val c = b.center
        assertEquals(20.0, c.x)
        assertEquals(30.0, c.y)
    }

    // ------------------------------------------------------------------
    // width / height
    // ------------------------------------------------------------------

    @Test
    fun `width and height of arbitrary rect`() {
        val b = Bounds(2.0, 3.0, 12.0, 8.0, CoordinateSpace.ScreenPx)
        assertEquals(10.0, b.width)
        assertEquals(5.0, b.height)
    }
}
