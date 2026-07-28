package com.androidvisualqa.capture.api

import com.androidvisualqa.model.ids.DraftId

/**
 * Single-entry reducer for the capture state machine.
 *
 * Every state transition is modeled as a pure (state, command) → state
 * function.  The caller is responsible for serializing access through a
 * [kotlinx.coroutines.channels.Channel] or [kotlinx.coroutines.sync.Mutex].
 */
public object CaptureReducer {

    /**
     * Reduce [command] against [state] and return the next state.
     *
     * @throws IllegalStateException if the transition is not defined.
     */
    public suspend fun reduce(state: CaptureState, command: CaptureCommand): CaptureState {
        return when (command) {
            is CaptureCommand.Trigger -> onTrigger(state, command)
            is CaptureCommand.ContextReady -> onContextReady(state, command)
            is CaptureCommand.PixelsReady -> onPixelsReady(state, command)
            is CaptureCommand.CaptureFailed -> onCaptureFailed(state, command)
            is CaptureCommand.UserCancelled -> onUserCancelled(state)
            is CaptureCommand.EditorSaved -> onEditorSaved(state, command)
        }
    }

    // -- private transitions -------------------------------------------------

    private fun onTrigger(state: CaptureState, command: CaptureCommand.Trigger): CaptureState {
        // ponytail: second Trigger from Armed is a no-op (returns same state).
        // This is safer for race conditions than throwing; the caller can
        // always check the result if they need to surface it to the user.
        if (state is CaptureState.Armed) return state

        return requireState<CaptureState.Idle>(state) {
            "Trigger is only valid from Idle, but current state is ${state::class.simpleName}"
        }.let {
            CaptureState.Armed
        }
    }

    private fun onContextReady(state: CaptureState, command: CaptureCommand.ContextReady): CaptureState {
        requireState<CaptureState.Armed>(state) {
            "ContextReady is only valid from Armed, but current state is ${state::class.simpleName}"
        }
        return CaptureState.SnapshottingContext
    }

    private fun onPixelsReady(state: CaptureState, command: CaptureCommand.PixelsReady): CaptureState {
        return when (state) {
            is CaptureState.SnapshottingContext -> CaptureState.ValidatingFrame
            is CaptureState.ValidatingFrame -> CaptureState.PersistingDraft
            // TODO(m2): handle target-window-change between tree and pixels
            else -> illegalTransition(state, "PixelsReady")
        }
    }

    private fun onCaptureFailed(state: CaptureState, command: CaptureCommand.CaptureFailed): CaptureState {
        if (state.isTerminal()) {
            throw IllegalStateException(
                "CaptureFailed is not valid from terminal state ${state::class.simpleName}"
            )
        }
        val recoverable = command.reason.isRecoverable()
        return CaptureState.Failed(reason = command.reason, recoverable = recoverable)
    }

    private fun onUserCancelled(state: CaptureState): CaptureState {
        if (state.isTerminal()) {
            throw IllegalStateException(
                "UserCancelled is not valid from terminal state ${state::class.simpleName}"
            )
        }
        return CaptureState.Cancelled
    }

    private fun onEditorSaved(state: CaptureState, command: CaptureCommand.EditorSaved): CaptureState {
        requireState<CaptureState.PersistingDraft>(state) {
            "EditorSaved is only valid from PersistingDraft, but current state is ${state::class.simpleName}"
        }
        return CaptureState.LaunchingEditor
    }

    // -- helpers --------------------------------------------------------------

    private fun CaptureState.isTerminal(): Boolean = this is CaptureState.Complete
            || this is CaptureState.Cancelled
            || this is CaptureState.Failed

    @PublishedApi
    internal inline fun <reified S : CaptureState> requireState(
        state: CaptureState,
        lazyMessage: () -> String,
    ): S {
        if (state !is S) throw IllegalStateException(lazyMessage())
        return state
    }

    private fun illegalTransition(state: CaptureState, commandName: String): Nothing {
        throw IllegalStateException(
            "$commandName is not valid from state ${state::class.simpleName}"
        )
    }
}

/**
 * Whether a failure is recoverable (can retry) or terminal.
 */
private fun CaptureFailure.isRecoverable(): Boolean = when (this) {
    is CaptureFailure.PermissionDenied -> false
    is CaptureFailure.MediaProjectionConsentCancelled -> false
    is CaptureFailure.ServiceDisconnected -> true
    is CaptureFailure.WindowUnavailable -> true
    is CaptureFailure.ScreenshotUnavailable -> true
    is CaptureFailure.StorageFailure -> false
    is CaptureFailure.Unknown -> false
}
