package com.androidvisualqa.annotation

/** A point whose coordinates are fractions of the original bitmap size. */
public data class NormalizedPoint(
    val x: Float,
    val y: Float,
) {
    public companion object {
        public fun from(x: Float, y: Float): NormalizedPoint =
            NormalizedPoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
    }
}

/** A normalized, ordered rectangle. Use [from] when corners are unordered. */
public data class NormalizedBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f)
        require(left <= right && top <= bottom)
    }

    public companion object {
        public fun from(x1: Float, y1: Float, x2: Float, y2: Float): NormalizedBounds =
            NormalizedBounds(
                minOf(x1, x2).coerceIn(0f, 1f),
                minOf(y1, y2).coerceIn(0f, 1f),
                maxOf(x1, x2).coerceIn(0f, 1f),
                maxOf(y1, y2).coerceIn(0f, 1f),
            )

        public val Empty: NormalizedBounds = NormalizedBounds(0f, 0f, 0f, 0f)
    }

    public fun contains(point: NormalizedPoint): Boolean =
        point.x in left..right && point.y in top..bottom
}

/** Geometry captured by one editor item. All coordinates are normalized. */
public sealed interface AnnotationGeometry {
    public data class Region(val bounds: NormalizedBounds) : AnnotationGeometry

    /** A closed, freehand selection with a normalized bounds used for snapping and reports. */
    public data class Lasso(
        val points: List<NormalizedPoint>,
        val bounds: NormalizedBounds,
    ) : AnnotationGeometry

    public data class Freehand(val points: List<NormalizedPoint>) : AnnotationGeometry

    public data class Arrow(
        val start: NormalizedPoint,
        val end: NormalizedPoint,
    ) : AnnotationGeometry

    public data class TextNote(
        val position: NormalizedPoint,
        val text: String = "",
    ) : AnnotationGeometry

    public data class CommentMarker(val position: NormalizedPoint) : AnnotationGeometry
}

/** One selectable visual annotation and the comments attached to it. */
public data class AnnotationItem(
    val id: AnnotationId,
    val geometry: AnnotationGeometry,
    val comments: List<AnnotationComment> = emptyList(),
    val color: Long = 0xFF6750A4L,
) {
    public val bounds: NormalizedBounds
        get() = when (val value = geometry) {
            is AnnotationGeometry.Region -> value.bounds
            is AnnotationGeometry.Lasso -> value.bounds
            is AnnotationGeometry.Freehand -> value.points.bounds()
            is AnnotationGeometry.Arrow -> NormalizedBounds.from(
                value.start.x, value.start.y, value.end.x, value.end.y,
            )
            is AnnotationGeometry.TextNote -> NormalizedBounds.from(
                value.position.x - 0.02f, value.position.y - 0.02f,
                value.position.x + 0.02f, value.position.y + 0.02f,
            )
            is AnnotationGeometry.CommentMarker -> NormalizedBounds.from(
                value.position.x - 0.025f, value.position.y - 0.025f,
                value.position.x + 0.025f, value.position.y + 0.025f,
            )
        }

    public fun withComment(text: String, commentId: String = "comment-${id.value}"): AnnotationItem =
        copy(comments = comments + AnnotationComment(commentId, text))
}

/** A user-editable comment linked to exactly one [AnnotationItem]. */
public data class AnnotationComment(
    val id: String,
    val text: String,
)

/** Converts a legacy rectangle into a selectable editor item. */
public fun RectangleAnnotation.toAnnotationItem(): AnnotationItem = AnnotationItem(
    id = id,
    geometry = AnnotationGeometry.Region(NormalizedBounds.from(left, top, right, bottom)),
    color = color,
)

/** Converts a region annotation to the legacy rectangle callback shape. */
public fun AnnotationItem.toRectangleOrNull(): RectangleAnnotation? =
    when (val value = geometry) {
        is AnnotationGeometry.Region -> value.bounds
        is AnnotationGeometry.Lasso -> value.bounds
        else -> null
    }?.let { bounds ->
        RectangleAnnotation(
            id = id,
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom,
            color = color,
        )
    }

private fun List<NormalizedPoint>.bounds(): NormalizedBounds {
    if (isEmpty()) return NormalizedBounds.Empty
    return NormalizedBounds.from(
        minOf { it.x }, minOf { it.y }, maxOf { it.x }, maxOf { it.y },
    )
}

/** Snaps an approximate box to a captured accessibility entity when it is close enough. */
public fun snapRegion(
    raw: NormalizedBounds,
    candidates: List<NormalizedBounds>,
): NormalizedBounds {
    val edgeSnapped = NormalizedBounds.from(
        snapEdge(raw.left), snapEdge(raw.top), snapEdge(raw.right), snapEdge(raw.bottom),
    )
    val target = candidates
        .asSequence()
        .filter { it.right > it.left && it.bottom > it.top }
        .filter { it.left >= 0f && it.top >= 0f && it.right <= 1f && it.bottom <= 1f }
        .filter { it.left != 0f || it.top != 0f || it.right != 1f || it.bottom != 1f }
        .map { it to overlapRatio(edgeSnapped, it) }
        .filter { (candidate, ratio) -> ratio >= 0.35f || candidate.centerInside(edgeSnapped) }
        .minByOrNull { (candidate, ratio) -> (1f - ratio) + candidate.width() * candidate.height() * 0.1f }
        ?.first
    // ponytail: overlap heuristic handles arbitrary apps; replace with SDK hit-testing when exact nodes are available.
    return target ?: edgeSnapped
}

private fun snapEdge(value: Float): Float = when {
    value <= 0.03f -> 0f
    value >= 0.97f -> 1f
    else -> value
}

private fun overlapRatio(a: NormalizedBounds, b: NormalizedBounds): Float {
    val width = (minOf(a.right, b.right) - maxOf(a.left, b.left)).coerceAtLeast(0f)
    val height = (minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)).coerceAtLeast(0f)
    val intersection = width * height
    val smaller = minOf(a.width() * a.height(), b.width() * b.height())
    return if (smaller == 0f) 0f else intersection / smaller
}

private fun NormalizedBounds.centerInside(box: NormalizedBounds): Boolean {
    return box.contains(
        NormalizedPoint(
            (left + right) / 2f,
            (top + bottom) / 2f,
        ),
    )
}

private fun NormalizedBounds.width(): Float = right - left
private fun NormalizedBounds.height(): Float = bottom - top
