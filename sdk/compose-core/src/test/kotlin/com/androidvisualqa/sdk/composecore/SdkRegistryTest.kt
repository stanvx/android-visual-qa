package com.androidvisualqa.sdk.composecore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SdkRegistryTest {

    private lateinit var registry: InMemorySdkRegistry

    @BeforeEach
    fun setUp() {
        registry = InMemorySdkRegistry()
    }

    @Test
    fun `register returns true for new stableId`() {
        val descriptor = SdkComponentDescriptor(stableId = "test.1")
        assertTrue(registry.register(descriptor))
    }

    @Test
    fun `register returns false for duplicate stableId`() {
        val descriptor = SdkComponentDescriptor(stableId = "test.1")
        assertTrue(registry.register(descriptor))
        assertFalse(registry.register(descriptor))
    }

    @Test
    fun `get returns registered descriptor`() {
        val descriptor = SdkComponentDescriptor(stableId = "test.1", route = SdkRoute("home"))
        registry.register(descriptor)
        val retrieved = registry.get("test.1")
        assertNotNull(retrieved)
        assertEquals("test.1", retrieved?.stableId)
    }

    @Test
    fun `get returns null for unknown stableId`() {
        assertNull(registry.get("does.not.exist"))
    }

    @Test
    fun `unregister removes existing entry`() {
        registry.register(SdkComponentDescriptor(stableId = "test.1"))
        assertTrue(registry.unregister("test.1"))
        assertNull(registry.get("test.1"))
    }

    @Test
    fun `unregister returns false for unknown stableId`() {
        assertFalse(registry.unregister("does.not.exist"))
    }

    @Test
    fun `all returns all registered descriptors`() {
        val d1 = SdkComponentDescriptor(stableId = "a")
        val d2 = SdkComponentDescriptor(stableId = "b")
        registry.register(d1)
        registry.register(d2)
        assertEquals(setOf("a", "b"), registry.all().map { it.stableId }.toSet())
    }

    @Test
    fun `all returns empty list when nothing registered`() {
        assertTrue(registry.all().isEmpty())
    }

    @Test
    fun `thread safety - 100 concurrent registrations all present`() = runTest {
        val descriptors = (1..100).map { SdkComponentDescriptor(stableId = "concurrent.$it") }

        withContext(Dispatchers.Default) {
            val jobs = descriptors.map { d ->
                launch { registry.register(d) }
            }
            jobs.forEach { it.join() }
        }

        val all = registry.all()
        assertEquals(100, all.size)
        assertEquals((1..100).map { "concurrent.$it" }.toSet(), all.map { it.stableId }.toSet())
    }
}
