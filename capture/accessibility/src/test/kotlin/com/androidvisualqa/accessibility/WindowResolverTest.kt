package com.androidvisualqa.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [WindowResolver] using Robolectric test runner.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class WindowResolverTest {

    @Test
    fun `resolve returns root node via rootInActiveWindow fallback`() {
        val root = AccessibilityNodeInfo.obtain()
        val windowId = root.windowId.toLong()

        val service = FakeAccessibilityService(
            windowsParam = null,
            activeRoot = root,
        )

        val resolved = WindowResolver.resolve(service, windowId)

        assertNotNull(resolved)
        assertEquals(root, resolved)
    }

    @Test
    fun `resolve returns null for non-matching windowId`() {
        val root = AccessibilityNodeInfo.obtain()
        val windowId = root.windowId.toLong()

        val service = FakeAccessibilityService(
            windowsParam = null,
            activeRoot = root,
        )

        val resolved = WindowResolver.resolve(service, windowId + 1000)

        assertNull(resolved)
    }

    @Test
    fun `resolve returns null for empty service`() {
        val service = FakeAccessibilityService(
            windowsParam = null,
            activeRoot = null,
        )

        val resolved = WindowResolver.resolve(service, 1L)

        assertNull(resolved)
    }

    private open class FakeAccessibilityService(
        private val windowsParam: List<AccessibilityWindowInfo>?,
        private val activeRoot: AccessibilityNodeInfo?,
    ) : AccessibilityService() {

        @Suppress("OVERRIDE_DEPRECATION")
        override fun getWindows() = windowsParam

        override fun getRootInActiveWindow() = activeRoot

        override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
        override fun onInterrupt() {}
    }
}
