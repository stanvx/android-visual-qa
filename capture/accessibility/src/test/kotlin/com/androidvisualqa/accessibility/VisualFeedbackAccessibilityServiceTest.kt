package com.androidvisualqa.accessibility

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [VisualFeedbackAccessibilityService] event ring buffer.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class VisualFeedbackAccessibilityServiceTest {

    @Test
    fun `ring buffer retains most recent 64 events`() {
        val service = TestService()

        for (i in 0 until 70) {
            val event = AccessibilityEvent.obtain(
                AccessibilityEvent.TYPE_VIEW_CLICKED,
            ).apply { contentDescription = "event-$i" }
            service.onAccessibilityEvent(event)
        }

        val events = synchronized(service.recentEvents) {
            service.recentEvents.toList()
        }

        assertEquals(64, events.size.toLong())
        assertEquals("event-6", events.first().contentDescription)
        assertEquals("event-69", events.last().contentDescription)
    }

    @Test
    fun `ring buffer drops oldest events when full`() {
        val service = TestService()

        for (i in 0 until 64) {
            val event = AccessibilityEvent.obtain(
                AccessibilityEvent.TYPE_VIEW_CLICKED,
            ).apply { contentDescription = "A-$i" }
            service.onAccessibilityEvent(event)
        }

        val newEvent = AccessibilityEvent.obtain(
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
        ).apply { contentDescription = "B-0" }
        service.onAccessibilityEvent(newEvent)

        val events = synchronized(service.recentEvents) {
            service.recentEvents.toList()
        }

        assertEquals(64, events.size.toLong())
        assertEquals("A-1", events.first().contentDescription)
        assertEquals("B-0", events.last().contentDescription)
    }

    @Test
    fun `null events are ignored`() {
        val service = TestService()

        service.onAccessibilityEvent(null)
        service.onAccessibilityEvent(null)

        val events = synchronized(service.recentEvents) {
            service.recentEvents.toList()
        }

        assertEquals(0, events.size.toLong())
    }

    @Test
    fun `activeWindowId returns null before any WINDOW_STATE_CHANGED`() {
        val service = TestService()
        assertNull(service.activeWindowId())
    }

    @Test
    fun `non-WINDOW_STATE_CHANGED events do not update activeWindowId`() {
        val service = TestService()
        assertNull(service.activeWindowId())

        service.onAccessibilityEvent(
            AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_CLICKED),
        )
        assertNull(service.activeWindowId())
    }

    @Test
    fun `received event is an obtained copy`() {
        val service = TestService()

        val original = AccessibilityEvent.obtain(
            AccessibilityEvent.TYPE_VIEW_CLICKED,
        ).apply { contentDescription = "original" }

        service.onAccessibilityEvent(original)
        original.contentDescription = "mutated"

        val events = synchronized(service.recentEvents) {
            service.recentEvents.toList()
        }

        assertEquals(1, events.size.toLong())
        assertEquals("original", events.first().contentDescription)
    }

    private class TestService : VisualFeedbackAccessibilityService()
}
