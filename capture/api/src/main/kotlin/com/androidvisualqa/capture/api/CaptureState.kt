package com.androidvisualqa.capture.api

/**
 * States of the capture state machine.
 *
 * Transition rules:
 * ```
 * Idle ──Trigger──→ Armed
 * Armed ──ContextReady──→ SnapshottingContext
 * SnapshottingContext ──PixelsReady──→ CapturingPixels
 * CapturingPixels ──PixelsReady──→ PersistingDraft
 * PersistingDraft ──EditorSaved──→ LaunchingEditor
 * (any active state) ──UserCancelled──→ Cancelled
 * (any active state) ──CaptureFailed──→ Failed
 * ```
 */
public sealed interface CaptureState {

    /** Awaiting a trigger. Initial + reset state. */
    public data object Idle : CaptureState

    /** Trigger received; waiting for context snapshot. */
    public data object Armed : CaptureState

    /** Context received; waiting for pixel capture. */
    public data object SnapshottingContext : CaptureState

    /** Pixels received; performing atomic draft persistence. */
    public data object CapturingPixels : CaptureState

    /** Draft persisted; waiting for editor launch confirmation. */
    public data object PersistingDraft : CaptureState

    /** Editor activity is being launched. */
    public data object LaunchingEditor : CaptureState

    /** User is annotating the frozen frame. */
    public data object Annotating : CaptureState

    /** AI enrichment in progress. */
    public data object Enriching : CaptureState

    /** User is reviewing the final report. */
    public data object Reviewing : CaptureState

    /** Final save/persist in progress. */
    public data object Saving : CaptureState

    /** Export in progress. */
    public data object Exporting : CaptureState

    /** Report completed successfully. */
    public data object Complete : CaptureState

    /** User cancelled the session. */
    public data object Cancelled : CaptureState

    /** Session failed. [recoverable] controls whether retry is possible. */
    public data class Failed(
        val reason: CaptureFailure,
        val recoverable: Boolean,
    ) : CaptureState

    /** Session is being restored from a persisted draft. */
    public data object Resuming : CaptureState
}
