package com.androidvisualqa.export.jira

/**
 * Provider interface for creating Jira issues from visual feedback reports.
 *
 * M5 stub only — the real HTTP client implementation ships in M6.
 */
public interface JiraExporter {

    /**
     * Submit an issue to the configured Jira instance.
     *
     * @param request   Issue payload.
     * @param baseUrl   Base URL of the Jira instance (e.g. "https://your-domain.atlassian.net").
     * @param authToken Personal access token or API token.
     * @return [Result.success] containing the created issue key and URL,
     *         or [Result.failure] on transport or API error.
     */
    public suspend fun submit(
        request: JiraIssueRequest,
        baseUrl: String,
        authToken: String,
    ): Result<JiraIssueResponse>
}

/**
 * Response returned by a successful Jira issue creation.
 *
 * @property key  Issue key (e.g. "QA-42").
 * @property url  Browser URL of the created issue.
 */
public data class JiraIssueResponse(
    val key: String,
    val url: String,
)

/**
 * Stub implementation that always returns [Result.failure].
 *
 * Intentionally does nothing until the real HTTP-backed adapter ships in M6.
 * This lets the rest of the app compile against the [JiraExporter] contract
 * without introducing any networking dependency.
 */
public class JiraExporterStub : JiraExporter {
    override suspend fun submit(
        request: JiraIssueRequest,
        baseUrl: String,
        authToken: String,
    ): Result<JiraIssueResponse> = Result.failure(
        UnsupportedOperationException(
            "Jira export ships in M6; track in TODO "
                + "(https://github.com/stanvx/android-visual-qa/issues)"
        ),
    )
}
