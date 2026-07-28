package com.androidvisualqa.sdk.compose

import com.androidvisualqa.sdk.composecore.InMemorySdkRegistry
import com.androidvisualqa.sdk.composecore.SdkComponentDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM-only test that exercises the [InMemorySdkRegistry] directly, without
 * a Compose UI runtime. This replaces the Compose UI test approach (which
 * would require [androidx.compose.ui.test.junit4.createComposeRule] in the
 * `androidTest` source set).
 *
 * For a full integration test that verifies [Modifier.feedbackTarget]
 * registration works through the composition tree, see the `androidTest`
 * source set or run the sample app.
 */
class SdkRegistryIntegrationTest {

    @Test
    fun `register and retrieve descriptor`() {
        val registry = InMemorySdkRegistry()
        val descriptor = SdkComponentDescriptor(stableId = "my.button")

        val registered = registry.register(descriptor)
        assertTrue("Expected register to succeed", registered)

        val retrieved = registry.get("my.button")
        assertNotNull("Expected descriptor to be retrievable", retrieved)
        assertEquals("my.button", retrieved?.stableId)
    }

    @Test
    fun `register rejects duplicate stableId`() {
        val registry = InMemorySdkRegistry()
        val d1 = SdkComponentDescriptor(stableId = "dup")
        val d2 = SdkComponentDescriptor(stableId = "dup")

        assertTrue("First registration should succeed", registry.register(d1))
        assertTrue("Second registration should return false", !registry.register(d2))
    }

    @Test
    fun `unregister removes descriptor`() {
        val registry = InMemorySdkRegistry()
        registry.register(SdkComponentDescriptor(stableId = "remove.me"))

        assertTrue("Unregister should succeed", registry.unregister("remove.me"))
        assertNull("Descriptor should be gone", registry.get("remove.me"))
    }

    @Test
    fun `unregister returns false for missing id`() {
        val registry = InMemorySdkRegistry()
        assertTrue("Unregister of missing id should return false", !registry.unregister("nope"))
    }

    @Test
    fun `all returns all registered descriptors`() {
        val registry = InMemorySdkRegistry()
        registry.register(SdkComponentDescriptor(stableId = "a"))
        registry.register(SdkComponentDescriptor(stableId = "b"))
        registry.register(SdkComponentDescriptor(stableId = "c"))

        val all = registry.all()
        assertEquals(3, all.size)
        assertEquals(setOf("a", "b", "c"), all.map { it.stableId }.toSet())
    }

    @Test
    fun `update replaces descriptor bounds`() {
        val registry = InMemorySdkRegistry()
        val original = SdkComponentDescriptor(stableId = "my.button")
        registry.register(original)

        val updated = original.copy(
            boundsLeft = 10, boundsTop = 20,
            boundsRight = 110, boundsBottom = 50,
        )
        val updateResult = registry.update("my.button", updated)
        assertTrue("Expected update to succeed", updateResult)

        val retrieved = registry.get("my.button")
        assertNotNull("Expected descriptor after update", retrieved)
        val r = retrieved!!
        assertEquals(10, r.boundsLeft)
        assertEquals(20, r.boundsTop)
        assertEquals(110, r.boundsRight)
        assertEquals(50, r.boundsBottom)
    }

    @Test
    fun `update returns false for unknown stableId`() {
        val registry = InMemorySdkRegistry()
        val descriptor = SdkComponentDescriptor(stableId = "nope")
        val result = registry.update("nope", descriptor)
        assertTrue("Expected update to return false for missing id", !result)
    }

    @Test
    fun `register then update replaces not duplicates`() {
        val registry = InMemorySdkRegistry()
        val original = SdkComponentDescriptor(stableId = "only.one")
        assertTrue("First register", registry.register(original))

        val updated = original.copy(boundsRight = 200)
        assertTrue("Update should succeed", registry.update("only.one", updated))

        val all = registry.all()
        assertEquals(1, all.size)
        assertEquals(200, all[0].boundsRight)
    }
}
