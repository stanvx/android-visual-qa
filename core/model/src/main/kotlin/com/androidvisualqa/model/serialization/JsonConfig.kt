package com.androidvisualqa.model.serialization

import kotlinx.serialization.json.Json

/**
 * Shared [Json] instance for all report serialization.
 *
 * Settings enforce a clean wire format:
 * - `prettyPrint = false` — compact for storage and transport
 * - `encodeDefaults = true` — explicit defaults prevent schema drift
 * - `explicitNulls = false` — omit absent fields rather than writing null
 * - `classDiscriminator = "type"` — polymorphic type tag, never Kotlin class names
 */
val JsonConfig = Json {
    prettyPrint = false
    encodeDefaults = true
    explicitNulls = false
    classDiscriminator = "type"
}
