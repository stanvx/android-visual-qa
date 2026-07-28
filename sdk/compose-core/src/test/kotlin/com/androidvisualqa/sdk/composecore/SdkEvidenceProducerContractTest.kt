package com.androidvisualqa.sdk.composecore

import com.androidvisualqa.model.capture.SdkComponentSnapshot
import com.androidvisualqa.model.ids.SdkComponentId
import com.androidvisualqa.model.serialization.JsonConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Tests that [SdkEvidence] round-trips through [JsonConfig]
 * and that the schema is wire-stable.
 */
class SdkEvidenceProducerContractTest {

    private val fixedEvidence = SdkEvidence(
        components = listOf(
            SdkComponentSnapshot(
                componentId = SdkComponentId("checkout.pay"),
                componentType = "Button",
                route = "checkout/payment",
                boundsLeft = 100,
                boundsTop = 200,
                boundsRight = 300,
                boundsBottom = 250,
                role = "Button",
                testTag = "payButton",
            ),
        ),
        routes = listOf(SdkRoute("checkout/payment", mapOf("id" to "42"))),
        designSystemSnapshots = mapOf(
            "checkout.pay" to DesignSystemSnapshot(
                tokens = listOf(DesignSystemToken("color", "surface", "FF0066CC")),
            ),
        ),
        privacy = mapOf(
            "checkout.pay" to PrivacyClassification.Public,
        ),
        build = BuildMetadata(
            buildType = "debug",
            buildId = "1234",
            gitSha = "abc123def456",
            isDebuggable = true,
        ),
    )

    @Test
    fun `fake producer returns expected fixed evidence`() {
        val producer = FakeSdkEvidenceProducer(fixedEvidence)
        val evidence = runBlocking { producer.snapshot() }
        assertEquals(fixedEvidence, evidence)
    }

    @Test
    fun `SdkEvidence round-trips through JsonConfig`() {
        val json = JsonConfig.encodeToString(fixedEvidence)
        val restored = JsonConfig.decodeFromString<SdkEvidence>(json)
        assertEquals(fixedEvidence.components.size, restored.components.size)
        assertEquals(
            fixedEvidence.components.first().componentId,
            restored.components.first().componentId,
        )
        assertEquals(fixedEvidence.routes, restored.routes)
        assertEquals(fixedEvidence.build, restored.build)
    }

    @Test
    fun `designSystemSnapshots survive serialization`() {
        val json = JsonConfig.encodeToString(fixedEvidence)
        val restored = JsonConfig.decodeFromString<SdkEvidence>(json)
        assertNotNull(restored.designSystemSnapshots["checkout.pay"])
        assertEquals(
            "FF0066CC",
            restored.designSystemSnapshots["checkout.pay"]?.tokens?.first()?.value,
        )
    }

    @Test
    fun `privacy classifications survive serialization`() {
        val json = JsonConfig.encodeToString(fixedEvidence)
        val restored = JsonConfig.decodeFromString<SdkEvidence>(json)
        assertEquals(
            PrivacyClassification.Public,
            restored.privacy["checkout.pay"],
        )
    }

    @Test
    fun `serializing SdkEvidence does not crash on empty collections`() {
        val empty = SdkEvidence(
            components = emptyList(),
            routes = emptyList(),
            designSystemSnapshots = emptyMap(),
            privacy = emptyMap(),
            build = BuildMetadata(buildType = "release"),
        )
        val json = JsonConfig.encodeToString(empty)
        val restored = JsonConfig.decodeFromString<SdkEvidence>(json)
        assertEquals(0, restored.components.size)
    }
}

private class FakeSdkEvidenceProducer(
    private val evidence: SdkEvidence,
) : SdkEvidenceProducer {
    override suspend fun snapshot(): SdkEvidence = evidence
}
