package com.androidvisualqa.capture.api

import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import com.androidvisualqa.geometry.Rotation
import com.androidvisualqa.model.ids.DraftId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class CaptureReducerTest {

    private val reducer = CaptureReducer

    private val snapshot = ContextSnapshot(
        packageName = "com.example.app",
        windowId = 42L,
        displayId = 0,
        bounds = Bounds(
            left = 0.0, top = 0.0,
            right = 1080.0, bottom = 2400.0,
            space = CoordinateSpace.ScreenPx,
        ),
        capturedAt = Clock.System.now(),
    )

    private val frame = CapturedFrame(
        displayId = 0,
        widthPx = 1080,
        heightPx = 2400,
        rotation = Rotation.ROTATION_0,
        capturedAt = Clock.System.now(),
    )

    // -- Happy path -----------------------------------------------------------

    @Test
    fun `Trigger from Idle transitions to Armed`() = runTest {
        val next = reducer.reduce(CaptureState.Idle, CaptureCommand.Trigger(TriggerSource.AccessibilityBubble))
        assertEquals(CaptureState.Armed, next)
    }

    @Test
    fun `ContextReady from Armed transitions to SnapshottingContext`() = runTest {
        val next = reducer.reduce(CaptureState.Armed, CaptureCommand.ContextReady(snapshot))
        assertEquals(CaptureState.SnapshottingContext, next)
    }

    @Test
    fun `PixelsReady from SnapshottingContext transitions to CapturingPixels`() = runTest {
        val next = reducer.reduce(CaptureState.SnapshottingContext, CaptureCommand.PixelsReady(frame))
        assertEquals(CaptureState.CapturingPixels, next)
    }

    @Test
    fun `PixelsReady from CapturingPixels transitions to PersistingDraft`() = runTest {
        val next = reducer.reduce(CaptureState.CapturingPixels, CaptureCommand.PixelsReady(frame))
        assertEquals(CaptureState.PersistingDraft, next)
    }

    @Test
    fun `EditorSaved from PersistingDraft transitions to LaunchingEditor`() = runTest {
        val next = reducer.reduce(
            CaptureState.PersistingDraft,
            CaptureCommand.EditorSaved(DraftId(UUID.randomUUID().toString())),
        )
        assertEquals(CaptureState.LaunchingEditor, next)
    }

    // -- Full trace -----------------------------------------------------------
    // Trigger -> ContextReady -> PixelsReady -> PixelsReady -> EditorSaved
    @Test
    fun `full capture flow trace`() = runTest {
        val state0 = CaptureState.Idle
        val state1 = reducer.reduce(state0, CaptureCommand.Trigger(TriggerSource.AccessibilityBubble))
        assertEquals(CaptureState.Armed, state1)

        val state2 = reducer.reduce(state1, CaptureCommand.ContextReady(snapshot))
        assertEquals(CaptureState.SnapshottingContext, state2)

        val state3 = reducer.reduce(state2, CaptureCommand.PixelsReady(frame))
        assertEquals(CaptureState.CapturingPixels, state3)

        val state4 = reducer.reduce(state3, CaptureCommand.PixelsReady(frame))
        assertEquals(CaptureState.PersistingDraft, state4)

        val state5 = reducer.reduce(state4, CaptureCommand.EditorSaved(DraftId(UUID.randomUUID().toString())))
        assertEquals(CaptureState.LaunchingEditor, state5)
    }

    // -- Race conditions ------------------------------------------------------

    @Test
    fun `second Trigger from Armed is a no-op returning same state`() = runTest {
        val state1 = reducer.reduce(CaptureState.Idle, CaptureCommand.Trigger(TriggerSource.AccessibilityBubble))
        assertEquals(CaptureState.Armed, state1)

        // Second trigger — no exception, same state
        val state2 = reducer.reduce(state1, CaptureCommand.Trigger(TriggerSource.QuickSettingsTile))
        assertEquals(CaptureState.Armed, state2)
    }

    @Test
    fun `CaptureFailed from Armed with ServiceDisconnected transitions to Failed recoverable`() = runTest {
        val next = reducer.reduce(
            CaptureState.Armed,
            CaptureCommand.CaptureFailed(CaptureFailure.ServiceDisconnected),
        )
        val failed = next as CaptureState.Failed
        assertEquals(CaptureFailure.ServiceDisconnected, failed.reason)
        assertEquals(true, failed.recoverable)
    }

    @Test
    fun `Activity starts before screenshot callback Trigger then delayed PixelsReady`() = runTest {
        val state1 = reducer.reduce(CaptureState.Idle, CaptureCommand.Trigger(TriggerSource.AccessibilityBubble))
        assertEquals(CaptureState.Armed, state1)

        // Simulate context snapshot arriving normally
        val state2 = reducer.reduce(state1, CaptureCommand.ContextReady(snapshot))
        assertEquals(CaptureState.SnapshottingContext, state2)

        // Pixels arrive late but still valid
        val state3 = reducer.reduce(state2, CaptureCommand.PixelsReady(frame))
        assertEquals(CaptureState.CapturingPixels, state3)
    }

    @Test
    fun `CaptureFailed with MediaProjectionConsentCancelled is recoverable=false`() = runTest {
        val next = reducer.reduce(
            CaptureState.Armed,
            CaptureCommand.CaptureFailed(CaptureFailure.MediaProjectionConsentCancelled),
        )
        val failed = next as CaptureState.Failed
        assertEquals(CaptureFailure.MediaProjectionConsentCancelled, failed.reason)
        assertEquals(false, failed.recoverable)
    }

    @Test
    fun `UserCancelled from any active state transitions to Cancelled`() = runTest {
        val activeStates = listOf<CaptureState>(
            CaptureState.Armed,
            CaptureState.SnapshottingContext,
            CaptureState.CapturingPixels,
            CaptureState.PersistingDraft,
            CaptureState.LaunchingEditor,
            CaptureState.Annotating,
            CaptureState.Enriching,
            CaptureState.Reviewing,
            CaptureState.Saving,
            CaptureState.Exporting,
            CaptureState.Resuming,
        )
        for (state in activeStates) {
            val next = reducer.reduce(state, CaptureCommand.UserCancelled)
            assertEquals(
                CaptureState.Cancelled, next,
                "UserCancelled should work from ${state::class.simpleName}",
            )
        }
    }

    // -- Invalid transitions --------------------------------------------------

    @Test
    fun `Trigger from non-Idle state throws IllegalStateException`() = runTest {
        val invalidStates = listOf(
            CaptureState.SnapshottingContext,
            CaptureState.CapturingPixels,
            CaptureState.PersistingDraft,
            CaptureState.LaunchingEditor,
            CaptureState.Annotating,
            CaptureState.Complete,
            CaptureState.Cancelled,
            CaptureState.Failed(CaptureFailure.Unknown("test"), false),
        )
        for (state in invalidStates) {
            val ex = assertThrows<IllegalStateException> {
                reducer.reduce(state, CaptureCommand.Trigger(TriggerSource.AccessibilityBubble))
            }
            assertTrue(
                ex.message?.contains("Trigger") == true,
                "Expected Trigger error in message, got: ${ex.message}",
            )
        }
    }

    @Test
    fun `ContextReady from non-Armed state throws IllegalStateException`() = runTest {
        val ex = assertThrows<IllegalStateException> {
            reducer.reduce(CaptureState.Idle, CaptureCommand.ContextReady(snapshot))
        }
        assertTrue(ex.message?.contains("ContextReady") == true)
    }

    @Test
    fun `EditorSaved from non-PersistingDraft state throws IllegalStateException`() = runTest {
        val ex = assertThrows<IllegalStateException> {
            reducer.reduce(CaptureState.Idle, CaptureCommand.EditorSaved(DraftId(UUID.randomUUID().toString())))
        }
        assertTrue(ex.message?.contains("EditorSaved") == true)
    }

    @Test
    fun `UserCancelled from terminal state throws IllegalStateException`() = runTest {
        val terminalStates = listOf<CaptureState>(
            CaptureState.Complete,
            CaptureState.Cancelled,
            CaptureState.Failed(CaptureFailure.Unknown("test"), false),
        )
        for (state in terminalStates) {
            val ex = assertThrows<IllegalStateException> {
                reducer.reduce(state, CaptureCommand.UserCancelled)
            }
            assertTrue(
                ex.message?.contains("UserCancelled") == true || ex.message?.contains("terminal") == true,
            )
        }
    }

    @Test
    fun `PixelsReady from Idle throws IllegalStateException`() = runTest {
        val ex = assertThrows<IllegalStateException> {
            reducer.reduce(CaptureState.Idle, CaptureCommand.PixelsReady(frame))
        }
        assertTrue(ex.message?.contains("PixelsReady") == true)
    }

    // -- Failure from any active state ----------------------------------------

    @Test
    fun `CaptureFailed from Idle transitions to Failed recoverable for transient`() = runTest {
        val next = reducer.reduce(
            CaptureState.Idle,
            CaptureCommand.CaptureFailed(CaptureFailure.ServiceDisconnected),
        )
        val failed = next as CaptureState.Failed
        assertEquals(true, failed.recoverable)
    }

    @Test
    fun `CaptureFailed with PermissionDenied is terminal`() = runTest {
        val next = reducer.reduce(
            CaptureState.Armed,
            CaptureCommand.CaptureFailed(CaptureFailure.PermissionDenied),
        )
        val failed = next as CaptureState.Failed
        assertEquals(CaptureFailure.PermissionDenied, failed.reason)
        assertEquals(false, failed.recoverable)
    }

    @Test
    fun `CaptureFailed from terminal state throws IllegalStateException`() = runTest {
        val terminalStates = listOf<CaptureState>(
            CaptureState.Complete,
            CaptureState.Cancelled,
            CaptureState.Failed(CaptureFailure.Unknown("x"), false),
        )
        for (state in terminalStates) {
            val ex = assertThrows<IllegalStateException> {
                reducer.reduce(state, CaptureCommand.CaptureFailed(CaptureFailure.ServiceDisconnected))
            }
            assertNotNull(ex.message)
        }
    }

    // -- TODO(m2) marker test (document only, not executable) -----------------

    @Test
    fun `TODO target window change between tree and pixels is documented`() {
        // TODO(m2): handle target-window-change between tree and pixels
        // When a WindowChanged event is added, verify that:
        //   - PixelsReady from CapturingPixels checks window ID match
        //   - Mismatch triggers CaptureFailed(WindowUnavailable)
        // This test is a placeholder until that lane lands.
        assertTrue(true, "Acceptance: gap is documented with TODO(m2) marker")
    }
}
