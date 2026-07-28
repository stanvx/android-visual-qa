package com.androidvisualqa.export.jira

/**
 * Payload for creating a Jira issue via the REST API.
 *
 * @property projectKey  Jira project key (e.g. "QA").
 * @property summary     Issue summary (single line).
 * @property description Issue description (Markdown or Atlassian Document Format).
 * @property issueType   Issue type name (default "Bug").
 */
public data class JiraIssueRequest(
    val projectKey: String,
    val summary: String,
    val description: String,
    val issueType: String = "Bug",
)
