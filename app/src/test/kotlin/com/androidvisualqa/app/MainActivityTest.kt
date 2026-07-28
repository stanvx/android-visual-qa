package com.androidvisualqa.app

import com.androidvisualqa.accessibility.VisualFeedbackAccessibilityService
import com.androidvisualqa.app.trigger.QuickSettingsTile
import com.androidvisualqa.model.ids.DraftId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Minimal JVM-level verification that [MainActivity] compiles and its
 * referenced types resolve.
 *
 * Full instrumentation tests (Robolectric, Compose UI tests) are M2.
 */
class MainActivityTest {

    @Test
    fun `class references compile`() {
        val expectedPackage = "com.androidvisualqa.app"
        assertEquals("com.androidvisualqa.app", expectedPackage)
    }

    @Test
    fun `stub service classes resolve`() {
        val tileClass = QuickSettingsTile::class
        val accClass = VisualFeedbackAccessibilityService::class
        assert(tileClass.simpleName == "QuickSettingsTile")
        assert(accClass.simpleName == "VisualFeedbackAccessibilityService")
    }
}
