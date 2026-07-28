package com.androidvisualqa.geometry

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PolygonTest {

    @Test
    fun `area of unit square is 1`() {
        val sq = Polygon(
            listOf(
                screenPx(0.0, 0.0),
                screenPx(1.0, 0.0),
                screenPx(1.0, 1.0),
                screenPx(0.0, 1.0),
            ),
        )
        assertEquals(1.0, sq.area, 1e-9)
    }

    @Test
    fun `centroid of unit triangle`() {
        val tri = Polygon(
            listOf(
                screenPx(0.0, 0.0),
                screenPx(1.0, 0.0),
                screenPx(0.0, 1.0),
            ),
        )
        val c = tri.centroid
        assertEquals(1.0 / 3.0, c.x, 1e-9)
        assertEquals(1.0 / 3.0, c.y, 1e-9)
    }

    @Test
    fun `centroid of unit square`() {
        val sq = Polygon(
            listOf(
                screenPx(0.0, 0.0),
                screenPx(1.0, 0.0),
                screenPx(1.0, 1.0),
                screenPx(0.0, 1.0),
            ),
        )
        val c = sq.centroid
        assertEquals(0.5, c.x, 1e-9)
        assertEquals(0.5, c.y, 1e-9)
    }

    @Test
    fun `contains point inside square`() {
        val sq = Polygon(
            listOf(
                screenPx(0.0, 0.0),
                screenPx(10.0, 0.0),
                screenPx(10.0, 10.0),
                screenPx(0.0, 10.0),
            ),
        )
        assertTrue(sq.contains(screenPx(5.0, 5.0)))
    }

    @Test
    fun `contains point outside square`() {
        val sq = Polygon(
            listOf(
                screenPx(0.0, 0.0),
                screenPx(10.0, 0.0),
                screenPx(10.0, 10.0),
                screenPx(0.0, 10.0),
            ),
        )
        assertFalse(sq.contains(screenPx(15.0, 5.0)))
    }

    @Test
    fun `contains point on vertex`() {
        val sq = Polygon(
            listOf(
                screenPx(0.0, 0.0),
                screenPx(10.0, 0.0),
                screenPx(10.0, 10.0),
                screenPx(0.0, 10.0),
            ),
        )
        // Ray casting: point on vertex is ambiguous but our implementation
        // counts it as inside via the edge-crossing logic.
        assertTrue(sq.contains(screenPx(0.0, 0.0)))
    }

    @Test
    fun `area of triangle with known area`() {
        val tri = Polygon(
            listOf(
                screenPx(0.0, 0.0),
                screenPx(4.0, 0.0),
                screenPx(2.0, 3.0),
            ),
        )
        assertEquals(6.0, tri.area, 1e-9)
    }

    @Test
    fun `bounds of polygon matches minmax of vertices`() {
        val tri = Polygon(
            listOf(
                screenPx(2.0, 5.0),
                screenPx(10.0, 3.0),
                screenPx(6.0, 12.0),
            ),
        )
        val b = tri.bounds
        assertEquals(2.0, b.left)
        assertEquals(3.0, b.top)
        assertEquals(10.0, b.right)
        assertEquals(12.0, b.bottom)
    }
}
