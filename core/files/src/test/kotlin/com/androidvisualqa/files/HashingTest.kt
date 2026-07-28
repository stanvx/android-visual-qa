package com.androidvisualqa.files

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HashingTest {

    @Test
    fun `sha256 of abc matches known vector`() {
        val input = "abc".toByteArray()
        val expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        assertEquals(expected, Hashing.sha256(input))
    }

    @Test
    fun `sha256 of empty input`() {
        val input = ByteArray(0)
        val expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        assertEquals(expected, Hashing.sha256(input))
    }

    @Test
    fun `sha256 is deterministic`() {
        val input = "The quick brown fox jumps over the lazy dog".toByteArray()
        assertEquals(Hashing.sha256(input), Hashing.sha256(input))
    }
}
