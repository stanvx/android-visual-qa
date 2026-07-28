package com.androidvisualqa.testing

import com.androidvisualqa.model.serialization.JsonConfig
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Asserts that [value] survives a JSON round-trip (encode → decode) and
 * produces an equal object.
 *
 * Uses [JsonConfig] from `:core:model` for the serializer configuration.
 */
public inline fun <reified T : @Serializable Any> assertJsonRoundTrips(
    value: T,
    serializer: KSerializer<T>? = null,
): Unit {
    val json: Json = JsonConfig
    val actual: T = if (serializer != null) {
        json.decodeFromString(serializer, json.encodeToString(serializer, value))
    } else {
        @Suppress("UNCHECKED_CAST")
        val ser = kotlinx.serialization.serializer<T>() as KSerializer<T>
        json.decodeFromString(ser, json.encodeToString(ser, value))
    }
    assertEquals(value, actual, "JSON round-trip equality failed for ${value::class.simpleName}")
}
