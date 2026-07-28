package com.androidvisualqa.export.agent

/**
 * Converts pixel coordinates to normalised [0.0, 1.0] agent-friendly bounds.
 *
 * The caller **must** supply the canvas dimensions; this object has no
 * knowledge of the original capture dimensions.
 *
 * Values are clamped to [0.0, 1.0] so that out-of-bounds input never
 * produces invalid normalised output.
 */
object BoundsNormalizer {

    /**
     * Normalise integer pixel bounds against the given canvas dimensions.
     *
     * @param left   Left edge in pixels.
     * @param top    Top edge in pixels.
     * @param right  Right edge in pixels.
     * @param bottom Bottom edge in pixels.
     * @param canvasWidth  Width of the canvas in pixels (must be > 0).
     * @param canvasHeight Height of the canvas in pixels (must be > 0).
     */
    fun normalize(
        left: Int, top: Int, right: Int, bottom: Int,
        canvasWidth: Int, canvasHeight: Int,
    ): AgentBounds = normalize(
        left = left.toDouble(),
        top = top.toDouble(),
        right = right.toDouble(),
        bottom = bottom.toDouble(),
        canvasWidth = canvasWidth.toDouble(),
        canvasHeight = canvasHeight.toDouble(),
    )

    /**
     * Normalise double-precision pixel bounds against the given canvas dimensions.
     *
     * @param left   Left edge in pixels.
     * @param top    Top edge in pixels.
     * @param right  Right edge in pixels.
     * @param bottom Bottom edge in pixels.
     * @param canvasWidth  Width of the canvas in pixels (must be > 0).
     * @param canvasHeight Height of the canvas in pixels (must be > 0).
     */
    fun normalize(
        left: Double, top: Double, right: Double, bottom: Double,
        canvasWidth: Double, canvasHeight: Double,
    ): AgentBounds {
        require(canvasWidth > 0.0) { "canvasWidth must be > 0, got $canvasWidth" }
        require(canvasHeight > 0.0) { "canvasHeight must be > 0, got $canvasHeight" }

        return AgentBounds(
            left = (left / canvasWidth).coerceIn(0.0, 1.0),
            top = (top / canvasHeight).coerceIn(0.0, 1.0),
            right = (right / canvasWidth).coerceIn(0.0, 1.0),
            bottom = (bottom / canvasHeight).coerceIn(0.0, 1.0),
        )
    }
}
