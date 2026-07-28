package com.androidvisualqa.geometry

/**
 * An immutable 2D point carrying its [CoordinateSpace] as a type parameter.
 *
 * @param S The coordinate space this point lives in.
 * @param x X-coordinate value.
 * @param y Y-coordinate value.
 * @param space The space this point is expressed in.
 */
data class Point<out S : CoordinateSpace>(
    val x: Double,
    val y: Double,
    val space: S,
) {

    override fun toString(): String = "Point($x, $y, space=${space})"
}

// ---------------------------------------------------------------------------
// Factory functions — one per known space for ergonomic construction.
// ---------------------------------------------------------------------------

fun screenPx(x: Double, y: Double): Point<CoordinateSpace.ScreenPx> =
    Point(x, y, CoordinateSpace.ScreenPx)

fun windowPx(x: Double, y: Double): Point<CoordinateSpace.WindowPx> =
    Point(x, y, CoordinateSpace.WindowPx)

fun capturePx(x: Double, y: Double): Point<CoordinateSpace.CapturePx> =
    Point(x, y, CoordinateSpace.CapturePx)

fun editorPx(x: Double, y: Double): Point<CoordinateSpace.EditorPx> =
    Point(x, y, CoordinateSpace.EditorPx)

fun normalizedFrame(x: Double, y: Double): Point<CoordinateSpace.NormalizedFrame> =
    Point(x, y, CoordinateSpace.NormalizedFrame)
