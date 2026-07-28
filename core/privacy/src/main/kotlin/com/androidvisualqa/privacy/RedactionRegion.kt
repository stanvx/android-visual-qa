package com.androidvisualqa.privacy

/**
 * A rectangular region flagged or marked for redaction.
 *
 * All coordinates are normalized 0..1 relative to the captured frame,
 * making them stable across capture-time transforms and display rotations.
 *
 * @property left Normalized left edge (0.0 = leftmost, 1.0 = rightmost).
 * @property top Normalized top edge (0.0 = top, 1.0 = bottom).
 * @property right Normalized right edge.
 * @property bottom Normalized bottom edge.
 * @property sensitivity The classification of the redacted content.
 * @property reason Human-readable explanation for the redaction.
 */
data class RedactionRegion(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
    val sensitivity: Sensitivity,
    val reason: String,
) {
    init {
        require(left in 0.0..1.0) { "left must be in [0,1], was $left" }
        require(top in 0.0..1.0) { "top must be in [0,1], was $top" }
        require(right in 0.0..1.0) { "right must be in [0,1], was $right" }
        require(bottom in 0.0..1.0) { "bottom must be in [0,1], was $bottom" }
    }
}
