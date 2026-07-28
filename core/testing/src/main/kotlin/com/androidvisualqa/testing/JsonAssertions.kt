package com.androidvisualqa.testing

import com.androidvisualqa.core.model.JsonConfig
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals

/**
 * Asserts that [value] survives a JSON round-trip (encode → decode) and
 * produces an equal object.
 *
 * Uses [JsonConfig.json] from `:core:model` for the serializer configuration.
 * Only included when `:core:model` has landed.
 */
public inline fun <reified T : @Serializable Any> assertJsonRoundTrips(
    value: T,
    serializer: KSerializer<T>? = null,
): Unit {
    val json: Json = JsonConfig.json
    val actual: T = if (serializer != null) {
        json.decodeFromString(serializer, json.encodeToString(serializer, value))
    } else {
        json.decodeFromString(json.encodeToString(value))
    }
    assertEquals(value, actual, "JSON round-trip equality failed for ${value::class.simpleName}")
}
