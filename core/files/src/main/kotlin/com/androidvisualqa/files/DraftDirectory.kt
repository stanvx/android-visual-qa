package com.androidvisualqa.files

import com.androidvisualqa.model.ids.DraftId
import java.nio.file.Path

/**
 * Root directory layout for draft storage.
 *
 * All draft paths are rooted under `root / "drafts" / {id.value}`.
 *
 * @property root The base storage directory for all drafts.
 */
public data class DraftDirectory(public val root: Path) {

    /** Returns the directory for a specific draft. */
    public fun draftPath(id: DraftId): Path = root.resolve("drafts").resolve(id.value)

    /** Returns the path for an attachment within a draft. */
    public fun attachmentPath(id: DraftId, name: String): Path =
        draftPath(id).resolve("attachments").resolve(name)

    /** Returns the path for the draft manifest JSON. */
    public fun manifestPath(id: DraftId): Path = draftPath(id).resolve("draft.json")

    /** Returns the path for the original screenshot. */
    public fun originalImagePath(id: DraftId): Path = draftPath(id).resolve("original.png")

    /** Returns the path for the annotated screenshot. */
    public fun annotatedImagePath(id: DraftId): Path = draftPath(id).resolve("annotated.png")

    /** Returns the path for the final report JSON. */
    public fun reportJsonPath(id: DraftId): Path = draftPath(id).resolve("report.json")

    /** Returns the path for the human-readable Markdown report. */
    public fun reportMarkdownPath(id: DraftId): Path = draftPath(id).resolve("report.md")

    /** Returns the path for capture metadata and accessibility candidates. */
    public fun captureContextPath(id: DraftId): Path = draftPath(id).resolve("capture-context.json")
}
