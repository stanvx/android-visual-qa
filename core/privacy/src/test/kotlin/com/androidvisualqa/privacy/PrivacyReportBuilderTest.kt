package com.androidvisualqa.privacy

import com.androidvisualqa.model.privacy.SecureWindowResult as ModelSecureWindowResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PrivacyReportBuilderTest {

    private val builder = PrivacyReportBuilder()

    @Test
    fun `build produces complete PrivacyEvidence`() {
        val manualRedactions = listOf(
            RedactionRegion(
                left = 0.1, top = 0.1, right = 0.3, bottom = 0.3,
                sensitivity = Sensitivity.Pii,
                reason = "User hid an email"
            ),
            RedactionRegion(
                left = 0.5, top = 0.5, right = 0.8, bottom = 0.8,
                sensitivity = Sensitivity.Financial,
                reason = "User hid a card number"
            ),
        )
        val automaticRedactions = listOf(
            RedactionRegion(
                left = 0.0, top = 0.0, right = 1.0, bottom = 1.0,
                sensitivity = Sensitivity.Credentials,
                reason = "Auto-detected credentials in feedback text"
            ),
            RedactionRegion(
                left = 0.0, top = 0.0, right = 1.0, bottom = 1.0,
                sensitivity = Sensitivity.Financial,
                reason = "Auto-detected financial in feedback text"
            ),
        )
        val feedback = "My email is a@b.com and card is 4111-1111-1111-1111"

        val evidence = builder.build(
            secureResult = SecureWindowResult.Secure,
            manualRedactions = manualRedactions,
            automaticRedactions = automaticRedactions,
            feedback = feedback,
        )

        assertEquals(ModelSecureWindowResult.SecureWindow, evidence.secureWindowResult)
        assertEquals(automaticRedactions.size, evidence.automaticRedactions.size)
        assertEquals(manualRedactions.size, evidence.userRedactions.size)
        assertEquals("User hid an email", evidence.userRedactions[0].reason)
        assertEquals(0.1, evidence.userRedactions[0].normalizedLeft, 0.001)

        // excludedFields should include at least "email" and "credit_card" from the feedback
        assertEquals(true, evidence.excludedFields.contains("email"))
        assertEquals(true, evidence.excludedFields.contains("credit_card"))
    }

    @Test
    fun `build with NotSecure and empty lists`() {
        val evidence = builder.build(
            secureResult = SecureWindowResult.NotSecure,
            manualRedactions = emptyList(),
            automaticRedactions = emptyList(),
            feedback = "All good",
        )

        assertEquals(ModelSecureWindowResult.NotSecure, evidence.secureWindowResult)
        assertEquals(0, evidence.automaticRedactions.size)
        assertEquals(0, evidence.userRedactions.size)
        assertEquals(0, evidence.excludedFields.size)
    }
}
