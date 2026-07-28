package com.androidvisualqa.export.github

/**
 * Payload for creating a GitHub issue via the REST API.
 *
 * @property title   Issue title.
 * @property body    Issue body (Markdown).
 * @property labels  Labels to apply to the issue.
 */
public data class GitHubIssueRequest(
    val title: String,
    val body: String,
    val labels: List<String>,
)
