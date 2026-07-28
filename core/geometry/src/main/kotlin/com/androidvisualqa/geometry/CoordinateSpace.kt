package com.androidvisualqa.geometry

/**
 * Sealed interface marker for explicit coordinate spaces.
 *
 * Every [Point] and [Bounds] carries its space as a type parameter so that
 * mixing spaces is a compile-time error rather than a silent unit mismatch.
 */
sealed interface CoordinateSpace {

    /** Screen pixels — the raw pixel grid of the physical display. */
    data object ScreenPx : CoordinateSpace

    /** Pixels in the application window coordinate system (may differ from screen due to
     * system bars, cutouts, or multi-window). */
    data object WindowPx : CoordinateSpace

    /** Pixels in the captured image (e.g. the accessibility screenshot bitmap). */
    data object CapturePx : CoordinateSpace

    /** Pixels in the annotation editor's internal coordinate system. */
    data object EditorPx : CoordinateSpace

    /** Normalised frame coordinates in the range [0.0, 1.0]. */
    data object NormalizedFrame : CoordinateSpace
}
