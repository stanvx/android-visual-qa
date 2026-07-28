package com.androidvisualqa.capture.api

import com.androidvisualqa.model.capture.TriggerSource
import com.androidvisualqa.model.ids.DraftId

/**
 * Commands that drive the capture state machine.
 *
 * Every external callback (accessibility, screenshot, activity result,
 * persistence, user action) is converted into one of these commands and
 * dispatched through [CaptureReducer.reduce].
 */
public sealed interface CaptureCommand {

    /** User or system initiated a capture. */
    public data class Trigger(val source: TriggerSource) : CaptureCommand

    /** Accessibility tree snapshot is ready. */
    public data class ContextReady(val snapshot: ContextSnapshot) : CaptureCommand

    /** Pixel capture (screenshot) is ready. */
    public data class PixelsReady(val frame: CapturedFrame) : CaptureCommand

    /** A capture operation failed. */
    public data class CaptureFailed(val reason: CaptureFailure) : CaptureCommand

    /** User cancelled the session. */
    public data object UserCancelled : CaptureCommand

    /** Editor has persisted a draft and handed back a draft ID. */
    public data class EditorSaved(val draftId: DraftId) : CaptureCommand
}
