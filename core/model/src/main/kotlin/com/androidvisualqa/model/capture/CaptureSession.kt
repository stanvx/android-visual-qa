package com.androidvisualqa.model.capture

import com.androidvisualqa.model.ids.DraftId
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Metadata about a single capture session.
 *
 * @property sessionId Unique session identifier.
 * @property startedAt Monotonic wall-clock timestamp of session start.
 * @property triggerSource How the user initiated capture.
 * @property captureMode Still, recording, or manual-import.
 * @property state Current lifecycle state of the session.
 * @property draftId Reference to the persisted draft, if one was created.
 * @property failureReason Human-readable reason if the session failed.
 * @property wasUserCancelled True if the user explicitly cancelled.
 */
@Serializable
data class CaptureSession(
    val sessionId: String,
    val startedAt: Instant,
    val triggerSource: TriggerSource,
    val captureMode: CaptureMode,
    val state: SessionState,
    val draftId: DraftId? = null,
    val failureReason: String? = null,
    val wasUserCancelled: Boolean = false,
)

@Serializable
enum class TriggerSource {
    AccessibilityOverlay,
    QuickSettingsTile,
    NotificationAction,
    SdkApi,
    ManualImport,
}

@Serializable
enum class CaptureMode {
    Still,
    Recording,
    ManualImport,
}

@Serializable
enum class SessionState {
    Idle,
    Armed,
    SnapshottingContext,
    CapturingPixels,
    PersistingDraft,
    LaunchingEditor,
    Annotating,
    Enriching,
    Reviewing,
    Saving,
    Exporting,
    Complete,
    Cancelled,
    Failed,
}
