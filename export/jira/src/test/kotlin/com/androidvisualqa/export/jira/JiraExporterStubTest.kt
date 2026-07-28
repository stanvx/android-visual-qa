package com.androidvisualqa.export.jira

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Verifies that [JiraExporterStub] returns a clear, honest failure
 * indicating the adapter is deferred to M6.
 */
class JiraExporterStubTest {

    private val stub = JiraExporterStub()

    @Test
    fun `submit returns failure with UnsupportedOperationException`() = runBlocking {
        val request = JiraIssueRequest(
            projectKey = "QA",
            summary = "Test issue",
            description = "Description text",
        )
        val result = stub.submit(request, "https://example.atlassian.net", "fake-token")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertInstanceOf(UnsupportedOperationException::class.java, exception)
        assertEquals(
            "Jira export ships in M6; track in TODO "
                + "(https://github.com/stanvx/android-visual-qa/issues)",
            exception?.message,
        )
    }
}
