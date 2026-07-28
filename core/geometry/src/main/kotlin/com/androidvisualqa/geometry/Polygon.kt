package com.androidvisualqa.geometry

import kotlin.math.abs

/**
 * An immutable polygon defined by an ordered list of vertices.
 *
 * The polygon is implicitly closed — a segment connects the last vertex back
 * to the first.  Vertices are assumed to be in either clockwise or
 * counter-clockwise order; self-intersecting polygons produce undefined
 * results for [area] and [contains].
 */
data class Polygon<S : CoordinateSpace>(
    val points: List<Point<S>>,
) {

    init {
        require(points.size >= 3) { "Polygon requires at least 3 points, got ${points.size}" }
    }

    /** Signed area of the polygon using the shoelace formula. */
    val area: Double
        get() {
            var sum = 0.0
            val n = points.size
            for (i in 0 until n) {
                val j = (i + 1) % n
                sum += points[i].x * points[j].y - points[j].x * points[i].y
            }
            return abs(sum) / 2.0
        }

    /** Centroid (geometric centre) of the polygon. */
    val centroid: Point<S>
        get() {
            val n = points.size
            var cx = 0.0
            var cy = 0.0
            var signedArea = 0.0
            for (i in 0 until n) {
                val j = (i + 1) % n
                val a = points[i].x * points[j].y - points[j].x * points[i].y
                signedArea += a
                cx += (points[i].x + points[j].x) * a
                cy += (points[i].y + points[j].y) * a
            }
            signedArea /= 2.0
            val area6 = 6.0 * signedArea
            return Point(
                x = cx / area6,
                y = cy / area6,
                space = points.first().space,
            )
        }

    /** Axis-aligned bounding rectangle that encloses all vertices. */
    val bounds: Bounds<S>
        get() {
            val xs = points.map { it.x }
            val ys = points.map { it.y }
            return Bounds(
                left = xs.min(),
                top = ys.min(),
                right = xs.max(),
                bottom = ys.max(),
                space = points.first().space,
            )
        }

    /**
     * Returns `true` if [point] lies inside this polygon using the ray-casting
     * algorithm.
     *
     * Points exactly on an edge are considered inside.
     */
    fun contains(point: Point<S>): Boolean {
        val n = points.size
        var inside = false
        var j = n - 1
        for (i in 0 until n) {
            val xi = points[i].x
            val yi = points[i].y
            val xj = points[j].x
            val yj = points[j].y
            if ((yi > point.y) != (yj > point.y) &&
                point.x < (xj - xi) * (point.y - yi) / (yj - yi) + xi
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }
}
