package com.androidvisualqa.geometry

/**
 * An immutable axis-aligned bounding rectangle in a given [CoordinateSpace].
 *
 * Contract:
 * - `left <= right` and `top <= bottom` — callers are responsible for normalising
 *   on construction if their source data is not already sorted.
 * - Values outside the visible area of the space are representable; no clipping
 *   is performed by this type.
 */
data class Bounds<S : CoordinateSpace>(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
    val space: S,
) {

    init {
        require(left <= right) { "left ($left) must be <= right ($right)" }
        require(top <= bottom) { "top ($top) must be <= bottom ($bottom)" }
    }

    val width: Double get() = right - left

    val height: Double get() = bottom - top

    /** The geometric centre of this rectangle. */
    val center: Point<S>
        get() = Point(
            x = left + width / 2.0,
            y = top + height / 2.0,
            space = space,
        )

    /** True when the rectangle has zero area (width <= 0 or height <= 0). */
    val isEmpty: Boolean get() = width <= 0.0 || height <= 0.0

    // ------------------------------------------------------------------
    // Spatial queries
    // ------------------------------------------------------------------

    /**
     * Returns `true` if [other] shares any interior point with this rectangle.
     *
     * Touching edges are considered intersecting (zero-width/high intersections
     * are non-empty for the purpose of overlap detection).
     */
    fun intersects(other: Bounds<S>): Boolean =
        left <= other.right &&
            right >= other.left &&
            top <= other.bottom &&
            bottom >= other.top

    /**
     * Returns the intersection of this and [other] as a new [Bounds].
     *
     * If the rectangles do not intersect the result is degenerate (zero area),
     * produced by clamping `left <= right` and `top <= bottom` with `maxOf`/`minOf`.
     */
    fun intersection(other: Bounds<S>): Bounds<S> {
        val l = maxOf(left, other.left)
        val t = maxOf(top, other.top)
        val r = minOf(right, other.right)
        val b = minOf(bottom, other.bottom)
        // Degenerate but valid: clamp so left <= right and top <= bottom.
        return Bounds(
            left = l,
            top = t,
            right = maxOf(l, r),
            bottom = maxOf(t, b),
            space = space,
        )
    }

    /**
     * Returns `true` when [point] lies on or inside this rectangle.
     *
     * Points exactly on the edge are considered contained.
     */
    fun contains(point: Point<S>): Boolean =
        point.x in left..right &&
            point.y in top..bottom
}
