package com.androidvisualqa.report

import com.androidvisualqa.files.AtomicFileWriter
import com.androidvisualqa.model.VisualFeedbackReport
import com.androidvisualqa.model.serialization.JsonConfig
import kotlinx.serialization.encodeToString
import java.nio.file.Path

/**
 * Writes a [VisualFeedbackReport] to a compact JSON string or to file.
 */
public class JsonReportWriter {

    /**
     * Serializes [report] to a compact (pretty-print off) JSON string.
     */
    public fun write(report: VisualFeedbackReport): String =
        JsonConfig.encodeToString(report)

    /**
     * Atomically writes the serialized report to [path] via [AtomicFileWriter].
     */
    public suspend fun write(report: VisualFeedbackReport, path: Path): Result<Unit> {
        val content = write(report)
        return AtomicFileWriter.writeTextAtomically(path, content)
    }
}
