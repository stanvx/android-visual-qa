package com.androidvisualqa.model.attachment

import com.androidvisualqa.model.ids.AttachmentId
import kotlinx.serialization.Serializable

/**
 * Reference to an attachment stored alongside the report.
 *
 * Attachments are stored separately from the JSON report body.
 * This record provides metadata for listing and integrity checks.
 *
 * @property attachmentId Unique identifier for this attachment.
 * @property fileName On-disk filename (within the report directory).
 * @property mimeType MIME type of the attachment.
 * @property sizeBytes File size in bytes.
 * @property sha256Hex Hex-encoded SHA-256 hash of the file content.
 * @property role Semantic role (e.g. "original_screenshot", "annotated_image").
 */
@Serializable
data class AttachmentRef(
    val attachmentId: AttachmentId,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256Hex: String,
    val role: String,
)
