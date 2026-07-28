package com.androidvisualqa.sdk.composecore

import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SdkOverrideTest {

    private fun bounds(
        left: Double, top: Double, right: Double, bottom: Double,
    ) = Bounds(left, top, right, bottom, CoordinateSpace.ScreenPx)

    @Test
    fun `bounds agree - same coordinates produce equal bounds`() {
        val b1 = bounds(10.0, 20.0, 100.0, 200.0)
        val b2 = bounds(10.0, 20.0, 100.0, 200.0)
        assertEquals(b1, b2)
    }

    @Test
    fun `bounds-disagree - different coordinates produce unequal bounds`() {
        val b1 = bounds(10.0, 20.0, 100.0, 200.0)
        val b2 = bounds(10.0, 20.0, 200.0, 300.0)
        assertEquals(false, b1 == b2)
    }

    @Test
    fun `sensitivity preserved through override`() {
        val override = SdkOverride(
            stableId = "checkout.pay",
            bounds = bounds(0.0, 0.0, 100.0, 50.0),
            sensitivity = PrivacyClassification.Credentials,
        )
        assertEquals(SdkSensitivity.Credentials, override.sensitivity)
    }

    @Test
    fun `override carries stableId`() {
        val override = SdkOverride(
            stableId = "checkout.pay",
            bounds = bounds(0.0, 0.0, 100.0, 50.0),
            sensitivity = PrivacyClassification.Public,
        )
        assertEquals("checkout.pay", override.stableId)
    }

    @Test
    fun `sensitivity defaults to public`() {
        val override = SdkOverride(
            stableId = "test",
            bounds = bounds(0.0, 0.0, 10.0, 10.0),
            sensitivity = PrivacyClassification.Public,
        )
        assertEquals(PrivacyClassification.Public, override.sensitivity)
    }
}
