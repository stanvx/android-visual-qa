package com.androidvisualqa.model

import com.androidvisualqa.model.serialization.InstantAsIso8601Serializer
import com.androidvisualqa.model.serialization.JsonConfig
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InstantSerializerTest {

    @Serializable
    data class InstantContainer(
        @Serializable(with = InstantAsIso8601Serializer::class)
        val timestamp: Instant,
    )

    @Test
    fun `instant serializes as ISO-8601 string`() {
        val instant = Instant.parse("2026-07-28T12:00:00Z")
        val container = InstantContainer(timestamp = instant)

        val json = JsonConfig.encodeToString(container)

        // Should be a plain string, not an object
        assertEquals("""{"timestamp":"2026-07-28T12:00:00Z"}""", json)
    }

    @Test
    fun `instant deserializes from ISO-8601 string`() {
        val json = """{"timestamp":"2026-07-28T14:30:00.500Z"}"""

        val container = JsonConfig.decodeFromString<InstantContainer>(json)

        val expected = Instant.parse("2026-07-28T14:30:00.500Z")
        assertEquals(expected, container.timestamp)
    }
}
