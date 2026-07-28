package com.androidvisualqa.export.github

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Verifies that [GitHubExporterStub] returns a clear, honest failure
 * indicating the adapter is deferred to M6.
 */
class GitHubExporterStubTest {

    private val stub = GitHubExporterStub()

    @Test
    fun `submit returns failure with UnsupportedOperationException`() = runBlocking {
        val request = GitHubIssueRequest(
            title = "Test issue",
            body = "Body text",
            labels = listOf("bug"),
        )
        val result = stub.submit(request, "fake-token")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertInstanceOf(UnsupportedOperationException::class.java, exception)
        assertEquals(
            "GitHub export ships in M6; track in TODO "
                + "(https://github.com/stanvx/android-visual-qa/issues)",
            exception?.message,
        )
    }
}
