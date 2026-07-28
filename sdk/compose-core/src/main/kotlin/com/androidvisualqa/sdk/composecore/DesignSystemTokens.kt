package com.androidvisualqa.sdk.composecore

import kotlinx.serialization.Serializable

/**
 * A single design-system token attached to a component.
 *
 * @property role The design-system category (e.g. "color", "typography", "spacing").
 * @property key The token key (e.g. "surface", "bodyLarge", "space.400").
 * @property value The resolved value (e.g. "FF0066CC", "16sp").
 */
@Serializable
data class DesignSystemToken(
    val role: String,
    val key: String,
    val value: String,
)

/**
 * An immutable snapshot of all design-system tokens for one component.
 *
 * @property tokens The list of resolved tokens at capture time.
 */
@Serializable
data class DesignSystemSnapshot(
    val tokens: List<DesignSystemToken>,
)
