package com.androidvisualqa.export.share

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

/**
 * Pure-JVM helper for building share and preview intents.
 *
 * All factory methods are stateless — no Android dependencies beyond [Intent]
 * and [Uri], making them suitable for JVM-unit-testable helper logic.
 */
public object ShareIntentBuilder {

    /**
     * Builds an [Intent.ACTION_SEND] intent suitable for a share sheet.
     *
     * The [uri] is placed as [Intent.EXTRA_STREAM] and the mime type is set
     * on the intent. [Intent.FLAG_GRANT_READ_URI_PERMISSION] is applied via
     * [IntentCompat] (on API 30+) or directly on older platforms.
     *
     * @param uri      The content URI to share.
     * @param mimeType MIME type for the content (default `"application/zip"`).
     * @return A share intent ready to be passed to [Intent.createChooser].
     */
    public fun buildShareIntent(
        uri: Uri,
        mimeType: String = "application/zip",
    ): Intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    /**
     * Wraps [intent] inside an [Intent.createChooser] with the given [title].
     *
     * @param intent The source share intent.
     * @param title  Title for the chooser dialog.
     * @return A chooser intent.
     */
    public fun buildChooserIntent(
        intent: Intent,
        title: String,
    ): Intent = Intent.createChooser(intent, title)

    /**
     * Builds an [Intent.ACTION_VIEW] intent for previewing the file at [uri].
     *
     * The caller must ensure that the [uri] is accessible (e.g. via
     * [FileProvider] or a content scheme). `FLAG_GRANT_READ_URI_PERMISSION`
     * is applied so the receiving activity can read the content.
     *
     * @param uri      The content URI to preview.
     * @param mimeType MIME type for the content.
     * @return A view intent.
     */
    public fun buildPreviewIntent(
        uri: Uri,
        mimeType: String,
    ): Intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
