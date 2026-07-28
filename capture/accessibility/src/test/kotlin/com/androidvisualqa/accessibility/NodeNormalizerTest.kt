package com.androidvisualqa.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.androidvisualqa.model.ids.NodeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [NodeNormalizer].
 *
 * Uses Robolectric test runner to provide real [AccessibilityNodeInfo] instances.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class NodeNormalizerTest {

    @Test
    fun `basic fields are extracted`() {
        val info = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.Button"
            viewIdResourceName = "com.example:id/my_button"
            isClickable = true
            isFocusable = true
            isEnabled = true
            isPassword = false
            isScrollable = false
            isVisibleToUser = true
            text = "Submit"
            contentDescription = "Submit button"
            setBoundsInScreen(Rect(10, 20, 100, 50))
        }

        val parentId = NodeId("parent-1")
        val snapshot = NodeNormalizer.normalize(info, parentId)

        assertEquals(parentId, snapshot.parentId)
        assertEquals("android.widget.Button", snapshot.className)
        assertEquals("com.example:id/my_button", snapshot.viewIdRaw)
        assertEquals("Submit", snapshot.text)
        assertEquals("Submit button", snapshot.contentDescription)
        assertEquals(10, snapshot.boundsLeft.toLong())
        assertEquals(20, snapshot.boundsTop.toLong())
        assertEquals(100, snapshot.boundsRight.toLong())
        assertEquals(50, snapshot.boundsBottom.toLong())
        assertTrue(snapshot.isClickable)
        assertTrue(snapshot.isFocusable)
        assertTrue(snapshot.isEnabled)
        assertFalse(snapshot.isPassword)
        assertFalse(snapshot.isScrollable)

        info.recycle()
    }

    @Test
    fun `text is truncated to 200 characters`() {
        val info = AccessibilityNodeInfo.obtain().apply {
            text = "A".repeat(500)
            setBoundsInScreen(Rect(0, 0, 10, 10))
        }

        val snapshot = NodeNormalizer.normalize(info, null)

        assertNotNull(snapshot.text)
        assertEquals(200, snapshot.text!!.length.toLong())
        assertEquals("A".repeat(200), snapshot.text)

        info.recycle()
    }

    @Test
    fun `content description is truncated to 200 characters`() {
        val info = AccessibilityNodeInfo.obtain().apply {
            contentDescription = "B".repeat(500)
            setBoundsInScreen(Rect(0, 0, 10, 10))
        }

        val snapshot = NodeNormalizer.normalize(info, null)

        assertNotNull(snapshot.contentDescription)
        assertEquals(200, snapshot.contentDescription!!.length.toLong())
        assertEquals("B".repeat(200), snapshot.contentDescription)

        info.recycle()
    }

    @Test
    fun `password field text is null`() {
        val info = AccessibilityNodeInfo.obtain().apply {
            isPassword = true
            text = "secret123"
            setBoundsInScreen(Rect(0, 0, 10, 10))
        }

        val snapshot = NodeNormalizer.normalize(info, null)

        assertNull(snapshot.text)

        info.recycle()
    }

    @Test
    fun `bounds are correctly mapped`() {
        val info = AccessibilityNodeInfo.obtain().apply {
            setBoundsInScreen(Rect(50, 100, 200, 300))
            className = "android.view.View"
        }

        val snapshot = NodeNormalizer.normalize(info, null)

        assertEquals(50, snapshot.boundsLeft.toLong())
        assertEquals(100, snapshot.boundsTop.toLong())
        assertEquals(200, snapshot.boundsRight.toLong())
        assertEquals(300, snapshot.boundsBottom.toLong())

        info.recycle()
    }

    @Test
    fun `clickable focusable flags pass through`() {
        val info = AccessibilityNodeInfo.obtain().apply {
            isClickable = true
            isFocusable = true
            isScrollable = true
            setBoundsInScreen(Rect(0, 0, 10, 10))
        }

        val snapshot = NodeNormalizer.normalize(info, null)

        assertTrue(snapshot.isClickable)
        assertTrue(snapshot.isFocusable)
        assertTrue(snapshot.isScrollable)

        info.recycle()
    }

    companion object {
        private fun assertFalse(value: Boolean) {
            org.junit.Assert.assertFalse(value)
        }
    }
}
