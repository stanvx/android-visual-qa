package com.androidvisualqa.sdk.composecore

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BuildMetadataTest {

    @Test
    fun `equality - same values are equal`() {
        val a = BuildMetadata(buildType = "debug", buildId = "42", gitSha = "abc", isDebuggable = true)
        val b = BuildMetadata(buildType = "debug", buildId = "42", gitSha = "abc", isDebuggable = true)
        assertEquals(a, b)
    }

    @Test
    fun `equality - different buildTypes are not equal`() {
        val a = BuildMetadata(buildType = "debug")
        val b = BuildMetadata(buildType = "release")
        assertEquals(false, a == b)
    }

    @Test
    fun `all-null metadata - buildId gitSha are null by default`() {
        val meta = BuildMetadata(buildType = "debug")
        assertNull(meta.buildId)
        assertNull(meta.gitSha)
    }

    @Test
    fun `debuggable flag is false by default`() {
        val meta = BuildMetadata(buildType = "release")
        assertFalse(meta.isDebuggable)
    }

    @Test
    fun `debuggable flag is true when set`() {
        val meta = BuildMetadata(buildType = "debug", isDebuggable = true)
        assertTrue(meta.isDebuggable)
    }

    @Test
    fun `buildType is required and preserved`() {
        val meta = BuildMetadata(buildType = "staging")
        assertEquals("staging", meta.buildType)
    }

    @Test
    fun `copy with modified field`() {
        val meta = BuildMetadata(buildType = "debug", isDebuggable = true)
        val release = meta.copy(buildType = "release", isDebuggable = false)
        assertEquals("release", release.buildType)
        assertFalse(release.isDebuggable)
    }

    @Test
    fun `toString does not crash`() {
        val meta = BuildMetadata(buildType = "debug", buildId = "1", gitSha = "abc123")
        assertTrue(meta.toString().contains("debug"))
    }
}
