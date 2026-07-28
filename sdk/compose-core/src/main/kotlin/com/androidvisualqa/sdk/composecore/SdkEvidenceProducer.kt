package com.androidvisualqa.sdk.composecore

import com.androidvisualqa.model.capture.SdkComponentSnapshot
import kotlinx.serialization.Serializable

/**
 * The contract the SDK uses to emit richer evidence for the matching and report engines.
 *
 * The concrete implementation lives in [:sdk:compose](https://github.com/stanvx/android-visual-qa/tree/main/sdk/compose)
 * and is consumed by [:app](https://github.com/stanvx/android-visual-qa/tree/main/app) and
 * [:capture:accessibility](https://github.com/stanvx/android-visual-qa/tree/main/capture/accessibility)
 * as a component-registration override path.
 */
interface SdkEvidenceProducer {
    /** Produce a snapshot of all current SDK evidence. */
    suspend fun snapshot(): SdkEvidence
}

/**
 * A snapshot of all SDK-provided evidence at a point in time.
 *
 * @property components The list of component snapshots matching the schema.
 * @property routes The list of active navigation routes.
 * @property designSystemSnapshots Map of stable ID to design-system snapshot.
 * @property privacy Map of stable ID to privacy classification.
 * @property build Build metadata for the host application.
 */
@Serializable
data class SdkEvidence(
    val components: List<SdkComponentSnapshot>,
    val routes: List<SdkRoute>,
    val designSystemSnapshots: Map<String, DesignSystemSnapshot>,
    val privacy: Map<String, PrivacyClassification>,
    val build: BuildMetadata,
)
