package com.androidvisualqa.annotation

/**
 * A single rectangle annotation in normalised bitmap coordinates (0..1).
 *
 * Left/top is the drag-start corner, right/bottom is the drag-end corner.
 * The caller normalises so left <= right and top <= bottom.
 *
 * @property id Local annotation identifier for undo/redo indexing.
 * @property left X of the near-left edge as a fraction of bitmap width.
 * @property top Y of the near-top edge as a fraction of bitmap height.
 * @property right X of the far-right edge as a fraction of bitmap width.
 * @property bottom Y of the far-bottom edge as a fraction of bitmap height.
 * @property color ARGB colour for the stroke as a [Long].
 */
public data class RectangleAnnotation(
    val id: AnnotationId,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val color: Long,
)
