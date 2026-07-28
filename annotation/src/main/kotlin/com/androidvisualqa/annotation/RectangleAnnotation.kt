package com.androidvisualqa.annotation

/**
 * A single rectangle annotation in bitmap-pixel coordinates.
 *
 * Left/top is the drag-start corner, right/bottom is the drag-end corner.
 * The caller normalises so left <= right and top <= bottom.
 *
 * @property id Local annotation identifier for undo/redo indexing.
 * @property left X of the near-left edge in bitmap-pixel space.
 * @property top Y of the near-top edge in bitmap-pixel space.
 * @property right X of the far-right edge in bitmap-pixel space.
 * @property bottom Y of the far-bottom edge in bitmap-pixel space.
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
