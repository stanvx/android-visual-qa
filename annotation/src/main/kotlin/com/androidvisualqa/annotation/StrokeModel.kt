package com.androidvisualqa.annotation

/**
 * A single polyline stroke made of ordered 2D points.
 *
 * This is the in-house stroke model used when AndroidX Ink is not available,
 * or as a lightweight transport model between editor Canvas pointer events
 * and the rendering layer.
 *
 * // ponytail: AndroidX Ink 1.0.0 IS available on Maven Central, but for M1
 * // the rectangle tool is a simple tap-drag-release gesture that produces
 * // exactly two corners. An Ink Stroke is overkill here. If M2 adds freehand
 * // (pen, highlighter, lasso) this model is replaced by InkStroke — the
 * // in-house points list becomes an intermediary during gesture accumulation.
 * // Upgrade path: replace Stroke.points with an InkStroke.Builder, call
 * // build() on pointer-up, and let InkRenderer handle Canvas drawing.
 */
public data class Stroke(
    val points: List<OffsetF> = emptyList(),
)

/**
 * Float-precision 2D offset used for stroke points.
 *
 * Kotlin's [androidx.compose.ui.geometry.Offset] is Compose-specific.
 * This pure-Kotlin type keeps [Stroke] usable from JVM tests and the
 * non-Compose model layer.
 */
public data class OffsetF(val x: Float, val y: Float) {
    /** Convenience constructor from a pair of floats. */
    public companion object {
        public fun xy(x: Float, y: Float): OffsetF = OffsetF(x, y)
    }
}
