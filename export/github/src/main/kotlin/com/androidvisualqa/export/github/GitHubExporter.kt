package com.androidvisualqa.export.github

/**
 * Provider interface for creating GitHub issues from visual feedback reports.
 *
 * M5 stub only — the real HTTP client implementation ships in M6.
 */
public interface GitHubExporter {

    /**
     * Submit an issue to the configured GitHub repository.
     *
     * @param request   Issue payload.
     * @param authToken Personal access token or OAuth token.
     * @return [Result.success] containing the created issue URL and number,
     *         or [Result.failure] on transport or API error.
     */
    public suspend fun submit(
        request: GitHubIssueRequest,
        authToken: String,
    ): Result<GitHubIssueResponse>
}

/**
 * Response returned by a successful issue creation.
 *
 * @property url    API URL of the created issue.
 * @property number Issue number assigned by GitHub.
 */
public data class GitHubIssueResponse(
    val url: String,
    val number: Int,
)

/**
 * Stub implementation that always returns [Result.failure].
 *
 * Intentionally does nothing until the real HTTP-backed adapter ships in M6.
 * This lets the rest of the app compile against the [GitHubExporter] contract
 * without introducing any networking dependency.
 */
public class GitHubExporterStub : GitHubExporter {
    override suspend fun submit(
        request: GitHubIssueRequest,
        authToken: String,
    ): Result<GitHubIssueResponse> = Result.failure(
        UnsupportedOperationException(
            "GitHub export ships in M6; track in TODO "
                + "(https://github.com/stanvx/android-visual-qa/issues)"
        ),
    )
}
