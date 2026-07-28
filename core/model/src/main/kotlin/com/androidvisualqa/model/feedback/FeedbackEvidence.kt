package com.androidvisualqa.model.feedback

import kotlinx.serialization.Serializable

/**
 * The user's typed or transcribed feedback for a report.
 *
 * @property textBody Free-form feedback text.
 * @property transcript Generated transcript if speech-to-text was used.
 * @property voiceNoteAttachmentId Reference to an optional voice note recording.
 * @property locale IETF BCP 47 language tag (e.g. "en-AU").
 */
@Serializable
data class FeedbackEvidence(
    val textBody: String? = null,
    val transcript: String? = null,
    val voiceNoteAttachmentId: String? = null,
    val locale: String? = null,
)
