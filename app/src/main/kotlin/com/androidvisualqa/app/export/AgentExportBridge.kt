package com.androidvisualqa.app.export

import android.content.Intent
import android.net.Uri
import com.androidvisualqa.export.agent.AgentBundleBuilder
import com.androidvisualqa.export.share.FileProviderWriter
import com.androidvisualqa.export.share.ShareIntentBuilder
import com.androidvisualqa.model.VisualFeedbackReport

/**
 * Orchestrates the export of a [VisualFeedbackReport] as an agent-friendly
 * JSON bundle for consumption by downstream AI tools (Claude, GPT, etc.).
 *
 * ## Export Agent Bundle flow
 *
 * 1. Calls [AgentBundleBuilder.buildJson] to serialise the report.
 * 2. Writes the JSON to a FileProvider-visible cache location via [FileProviderWriter].
 * 3. Returns the content URI (and optionally a share [Intent]).
 *
 * @param agentBundleBuilder  Builder that produces the agent JSON bundle.
 * @param fileProviderWriter  Writer for FileProvider-visible cache locations.
 */
public class AgentExportBridge(
    private val agentBundleBuilder: AgentBundleBuilder,
    private val fileProviderWriter: FileProviderWriter,
) {

    /**
     * Builds the agent bundle JSON and writes it to a FileProvider-visible
     * cache file, returning a `content://` URI.
     *
     * @param report The report to export.
     * @return [Result.success] with the FileProvider [Uri].
     */
    public suspend fun exportAgentBundle(report: VisualFeedbackReport): Result<Uri> {
        val json = agentBundleBuilder.buildJson(report)
        val bytes = json.toByteArray(Charsets.UTF_8)
        return fileProviderWriter.writeBytesToCache(
            subdir = "agent-exports",
            filename = "${report.reportId.value}.json",
            bytes = bytes,
        )
    }

    /**
     * Builds the agent bundle JSON, writes it to cache, and returns a
     * share [Intent] and the content URI.
     *
     * @param report The report to export.
     * @return [Result.success] with the [Uri] and share [Intent].
     */
    public suspend fun shareAgentBundle(report: VisualFeedbackReport): Result<Pair<Uri, Intent>> = runCatching {
        val uri = exportAgentBundle(report).getOrThrow()
        val intent = ShareIntentBuilder.buildShareIntent(uri, mimeType = "application/json")
        uri to intent
    }
}
