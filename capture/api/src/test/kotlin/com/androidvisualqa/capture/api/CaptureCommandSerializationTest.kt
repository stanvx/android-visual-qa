package com.androidvisualqa.capture.api

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Verifies that [CaptureCommand] types have stable [toString] representations
 * suitable for M0 logging.
 *
 * Full kotlinx.serialization round-trip is deferred: the command types carry
 * domain objects that depend on `:core:geometry` types which may not yet have
 * serialization annotations.  Add a kotlinx-serialization test when the
 * geometry and model modules define their own serializers.
 *
 * TODO(m1): add CaptureCommand kotlinx.serialization round-trip test after
 * geometry/model serializers are defined.
 */
class CaptureCommandSerializationTest {

    @Test
    fun `Trigger toString contains source`() {
        val cmd = CaptureCommand.Trigger(TriggerSource.AccessibilityBubble)
        val str = cmd.toString()
        assertNotNull(str)
        // toString should include the source type
        org.junit.jupiter.api.Assertions.assertTrue(
            str.contains("AccessibilityBubble"),
            "Trigger.toString should include source: $str",
        )
    }

    @Test
    fun `UserCancelled toString does not crash`() {
        val str = CaptureCommand.UserCancelled.toString()
        assertNotNull(str)
    }

    @Test
    fun `CaptureFailed toString contains reason`() {
        val cmd = CaptureCommand.CaptureFailed(CaptureFailure.ServiceDisconnected)
        val str = cmd.toString()
        assertNotNull(str)
        org.junit.jupiter.api.Assertions.assertTrue(
            str.contains("ServiceDisconnected"),
            "CaptureFailed.toString should include reason: $str",
        )
    }

    @Test
    fun `ContextReady toString does not crash`() {
        val now = kotlinx.datetime.Clock.System.now()
        val snapshot = ContextSnapshot(
            packageName = "com.example",
            windowId = 42L,
            displayId = 0,
            bounds = com.androidvisualqa.core.geometry.Bounds(
                com.androidvisualqa.core.geometry.ScreenPx(0f),
                com.androidvisualqa.core.geometry.ScreenPx(0f),
                com.androidvisualqa.core.geometry.ScreenPx(1080f),
                com.androidvisualqa.core.geometry.ScreenPx(2400f),
            ),
            capturedAt = now,
        )
        val cmd = CaptureCommand.ContextReady(snapshot)
        assertNotNull(cmd.toString())
    }

    @Test
    fun `PixelsReady toString does not crash`() {
        val now = kotlinx.datetime.Clock.System.now()
        val frame = CapturedFrame(
            displayId = 0,
            widthPx = 1080,
            heightPx = 2400,
            rotation = com.androidvisualqa.core.geometry.Rotation.Rotation_0,
            capturedAt = now,
        )
        val cmd = CaptureCommand.PixelsReady(frame)
        assertNotNull(cmd.toString())
    }

    @Test
    fun `EditorSaved toString contains draftId`() {
        val draftId = DraftId.random()
        val cmd = CaptureCommand.EditorSaved(draftId)
        val str = cmd.toString()
        assertNotNull(str)
        org.junit.jupiter.api.Assertions.assertTrue(
            str.contains(draftId.value),
            "EditorSaved.toString should include draftId: $str",
        )
    }

    @Test
    fun `all command types have non-null toString`() {
        val now = kotlinx.datetime.Clock.System.now()
        val commands = listOf(
            CaptureCommand.Trigger(TriggerSource.AccessibilityBubble),
            CaptureCommand.ContextReady(
                ContextSnapshot(
                    "com.example", 42L, 0,
                    com.androidvisualqa.core.geometry.Bounds(
                        com.androidvisualqa.core.geometry.ScreenPx(0f),
                        com.androidvisualqa.core.geometry.ScreenPx(0f),
                        com.androidvisualqa.core.geometry.ScreenPx(1080f),
                        com.androidvisualqa.core.geometry.ScreenPx(2400f),
                    ),
                    now,
                ),
            ),
            CaptureCommand.PixelsReady(CapturedFrame(0, 1080, 2400, com.androidvisualqa.core.geometry.Rotation.Rotation_0, now)),
            CaptureCommand.CaptureFailed(CaptureFailure.ServiceDisconnected),
            CaptureCommand.UserCancelled,
            CaptureCommand.EditorSaved(DraftId.random()),
        )
        for (cmd in commands) {
            assertNotNull(cmd.toString(), "toString must not return null for ${cmd::class.simpleName}")
        }
    }
}
