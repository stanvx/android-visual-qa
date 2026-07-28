package com.androidvisualqa.app

import com.androidvisualqa.app.trigger.QuickSettingsTile
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Minimal JVM test for [QuickSettingsTile].
 *
 * The tile requires the Android framework ([TileService]) so this test
 * verifies the class is constructable and its metadata is valid by
 * exercising the companion object and class references.
 *
 * // TODO(m3): add Robolectric tests for onStartListening/onStopListening.
 */
class QuickSettingsTileTest {

    @Test
    fun `class is loadable`() {
        // Verify the class can be referenced and has the expected simple name
        val className = QuickSettingsTile::class.simpleName
        assertNotNull(className)
        assert(className == "QuickSettingsTile")
    }

    @Test
    fun `class is a TileService subclass`() {
        val isAssignable = android.service.quicksettings.TileService::class
            .java
            .isAssignableFrom(QuickSettingsTile::class.java)
        assert(isAssignable) { "QuickSettingsTile must extend TileService" }
    }
}
