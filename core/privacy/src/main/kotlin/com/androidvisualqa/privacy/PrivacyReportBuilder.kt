package com.androidvisualqa.privacy

import com.androidvisualqa.model.privacy.PrivacyEvidence
import com.androidvisualqa.model.privacy.RedactionRegion as ModelRedactionRegion
import com.androidvisualqa.model.privacy.SecureWindowResult as ModelSecureWindowResult

/**
 * Builds a [PrivacyEvidence] report from the privacy subsystem's internal types.
 *
 * Converts the domain-level [RedactionRegion] (which carries a [Sensitivity]) into
 * the serialization-ready model [ModelRedactionRegion] and maps enum values.
 */
class PrivacyReportBuilder(
    private val classifier: SensitiveFieldClassifier = SensitiveFieldClassifier(),
) {

    /**
     * Constructs a [PrivacyEvidence] payload for a report.
     *
     * @param secureResult Whether the captured window had FLAG_SECURE.
     * @param manualRedactions Regions explicitly drawn by the user.
     * @param automaticRedactions Regions auto-suggested by [AutomaticRedactionSuggester].
     * @param feedback The free-form feedback text (used for excludedFields extraction).
     */
    fun build(
        secureResult: SecureWindowResult,
        manualRedactions: List<RedactionRegion>,
        automaticRedactions: List<RedactionRegion>,
        feedback: String,
    ): PrivacyEvidence {
        return PrivacyEvidence(
            secureWindowResult = mapSecureWindowResult(secureResult),
            automaticRedactions = automaticRedactions.map { mapRegion(it) },
            userRedactions = manualRedactions.map { mapRegion(it) },
            excludedFields = classifier.matchedFieldNames(feedback),
            ocrUsed = false,
            audioUsed = false,
            exportLeftDevice = false,
        )
    }

    private fun mapSecureWindowResult(result: SecureWindowResult): ModelSecureWindowResult =
        when (result) {
            SecureWindowResult.Secure -> ModelSecureWindowResult.SecureWindow
            SecureWindowResult.NotSecure -> ModelSecureWindowResult.NotSecure
            SecureWindowResult.Unknown -> ModelSecureWindowResult.Unknown
        }

    private fun mapRegion(region: RedactionRegion): ModelRedactionRegion =
        ModelRedactionRegion(
            reason = region.reason,
            normalizedLeft = region.left,
            normalizedTop = region.top,
            normalizedRight = region.right,
            normalizedBottom = region.bottom,
        )
}
