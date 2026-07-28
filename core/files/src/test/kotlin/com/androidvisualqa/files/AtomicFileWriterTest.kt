package com.androidvisualqa.files

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class AtomicFileWriterTest {

    @Test
    fun `happy path writes bytes correctly`(@TempDir tempDir: Path) = runTest {
        val target = tempDir.resolve("hello.txt")
        val bytes = "Hello, World!".toByteArray()

        val result = AtomicFileWriter.writeAtomically(target, bytes)

        assertTrue(result.isSuccess)
        assertArrayEquals(bytes, Files.readAllBytes(target))
    }

    @Test
    fun `overwrite replaces existing content`(@TempDir tempDir: Path) = runTest {
        val target = tempDir.resolve("overwrite.txt")
        Files.writeString(target, "old content")

        val newBytes = "new content".repeat(100).toByteArray()
        val result = AtomicFileWriter.writeAtomically(target, newBytes)

        assertTrue(result.isSuccess)
        assertArrayEquals(newBytes, Files.readAllBytes(target))
    }

    @Test
    fun `large 1MB write round-trips exactly`(@TempDir tempDir: Path) = runTest {
        val target = tempDir.resolve("large.bin")
        val bytes = ByteArray(1_024 * 1_024) { (it % 256).toByte() }

        val result = AtomicFileWriter.writeAtomically(target, bytes)

        assertTrue(result.isSuccess)
        assertArrayEquals(bytes, Files.readAllBytes(target))
    }

    @Test
    fun `writeTextAtomically round-trips`(@TempDir tempDir: Path) = runTest {
        val target = tempDir.resolve("text.txt")
        val text = "Hello, 世界"

        val result = AtomicFileWriter.writeTextAtomically(target, text)

        assertTrue(result.isSuccess)
        assertTrue(Files.readString(target) == text)
    }
}
