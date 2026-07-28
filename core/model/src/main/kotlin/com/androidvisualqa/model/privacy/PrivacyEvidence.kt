package com.androidvisualqa.model.privacy

import kotlinx.serialization.Serializable

/**
 * Privacy-related decisions and annotations for a report.
 *
 * Tracks what was redacted, how redaction was suggested, and whether
 * sensitive data left the device.
 *
 * @property secureWindowResult Whether the captured window had FLAG_SECURE.
 * @property automaticRedactions List of regions auto-suggested for redaction.
 * @property userRedactions List of regions manually redacted by the user.
 * @property excludedFields Field paths excluded from serialization.
 * @property ocrUsed Whether OCR was run on the captured frame.
 * @property audioUsed Whether audio/voice was captured.
 * @property exportLeftDevice Whether any export destination received data.
 */
@Serializable
data class PrivacyEvidence(
    val secureWindowResult: SecureWindowResult = SecureWindowResult.NotSecure,
    val automaticRedactions: List<RedactionRegion> = emptyList(),
    val userRedactions: List<RedactionRegion> = emptyList(),
    val excludedFields: List<String> = emptyList(),
    val ocrUsed: Boolean = false,
    val audioUsed: Boolean = false,
    val exportLeftDevice: Boolean = false,
)

@Serializable
enum class SecureWindowResult {
    NotSecure,
    SecureWindow,
    Unknown,
}

/**
 * A rectangular region flagged or marked for redaction.
 *
 * Coordinates are normalized (0.0..1.0) relative to the captured frame.
 *
 * @property reason Why this region was redacted.
 * @property normalizedLeft Bounding box in normalized coordinates.
 * @property normalizedTop Bounding box in normalized coordinates.
 * @property normalizedRight Bounding box in normalized coordinates.
 * @property normalizedBottom Bounding box in normalized coordinates.
 */
@Serializable
data class RedactionRegion(
    val reason: String,
    val normalizedLeft: Double,
    val normalizedTop: Double,
    val normalizedRight: Double,
    val normalizedBottom: Double,
)
