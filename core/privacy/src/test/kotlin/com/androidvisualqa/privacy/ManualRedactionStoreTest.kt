package com.androidvisualqa.privacy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test

class ManualRedactionStoreTest {

    private val store = ManualRedactionStore()

    private val region1 = RedactionRegion(
        left = 0.1, top = 0.1, right = 0.3, bottom = 0.3,
        sensitivity = Sensitivity.Pii, reason = "User redaction"
    )
    private val region2 = RedactionRegion(
        left = 0.5, top = 0.5, right = 0.8, bottom = 0.8,
        sensitivity = Sensitivity.Financial, reason = "Card number"
    )

    @Test
    fun `add and list regions`() {
        store.add(region1)
        store.add(region2)
        assertEquals(2, store.list().size)
    }

    @Test
    fun `remove region by index`() {
        store.add(region1)
        store.add(region2)
        store.remove(0)
        assertEquals(1, store.list().size)
        assertEquals(region2, store.list()[0])
    }

    @Test
    fun `clear removes all regions`() {
        store.add(region1)
        store.add(region2)
        store.clear()
        assertEquals(0, store.list().size)
    }

    @Test
    fun `list returns an immutable copy`() {
        store.add(region1)
        val list1 = store.list()
        store.add(region2)
        val list2 = store.list()
        assertEquals(1, list1.size)
        assertEquals(2, list2.size)
        assertNotSame(list1, list2)
    }
}
