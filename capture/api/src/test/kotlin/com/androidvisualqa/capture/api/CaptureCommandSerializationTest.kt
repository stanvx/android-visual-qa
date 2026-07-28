package com.androidvisualqa.capture.api

import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import com.androidvisualqa.geometry.Rotation
import com.androidvisualqa.model.ids.DraftId
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

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
        assertTrue(
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
        assertTrue(
            str.contains("ServiceDisconnected"),
            "CaptureFailed.toString should include reason: $str",
        )
    }

    @Test
    fun `ContextReady toString does not crash`() {
        val snapshot = ContextSnapshot(
            packageName = "com.example",
            windowId = 42L,
            displayId = 0,
            bounds = Bounds(
                left = 0.0, top = 0.0,
                right = 1080.0, bottom = 2400.0,
                space = CoordinateSpace.ScreenPx,
            ),
            capturedAt = Clock.System.now(),
        )
        val cmd = CaptureCommand.ContextReady(snapshot)
        assertNotNull(cmd.toString())
    }

    @Test
    fun `PixelsReady toString does not crash`() {
        val frame = CapturedFrame(
            displayId = 0,
            widthPx = 1080,
            heightPx = 2400,
            rotation = Rotation.ROTATION_0,
            capturedAt = Clock.System.now(),
        )
        val cmd = CaptureCommand.PixelsReady(frame)
        assertNotNull(cmd.toString())
    }

    @Test
    fun `EditorSaved toString contains draftId`() {
        val draftId = DraftId(UUID.randomUUID().toString())
        val cmd = CaptureCommand.EditorSaved(draftId)
        val str = cmd.toString()
        assertNotNull(str)
        assertTrue(
            str.contains(draftId.value),
            "EditorSaved.toString should include draftId: $str",
        )
    }

    @Test
    fun `all command types have non-null toString`() {
        val now = Clock.System.now()
        val commands = listOf(
            CaptureCommand.Trigger(TriggerSource.AccessibilityBubble),
            CaptureCommand.ContextReady(
                ContextSnapshot(
                    packageName = "com.example",
                    windowId = 42L,
                    displayId = 0,
                    bounds = Bounds(
                        left = 0.0, top = 0.0,
                        right = 1080.0, bottom = 2400.0,
                        space = CoordinateSpace.ScreenPx,
                    ),
                    capturedAt = now,
                ),
            ),
            CaptureCommand.PixelsReady(CapturedFrame(0, 1080, 2400, Rotation.ROTATION_0, now)),
            CaptureCommand.CaptureFailed(CaptureFailure.ServiceDisconnected),
            CaptureCommand.UserCancelled,
            CaptureCommand.EditorSaved(DraftId(UUID.randomUUID().toString())),
        )
        for (cmd in commands) {
            assertNotNull(
                cmd.toString(),
                "toString must not return null for ${cmd::class.simpleName}",
            )
        }
    }
}
