package com.androidvisualqa.export.share

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [ShareIntentBuilder].
 *
 * Uses Robolectric to provide Android framework classes ([Intent], [Uri]).
 * The builder itself is stateless pure logic, but the Android SDK classes
 * require a runtime environment.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [30])
class ShareIntentBuilderTest {

    private val uri: Uri = Uri.parse("content://com.androidvisualqa.app.fileprovider/exports/report.zip")

    @Test
    fun `buildShareIntent returns ACTION_SEND with URI as EXTRA_STREAM`() {
        val intent = ShareIntentBuilder.buildShareIntent(uri, "application/zip")

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("application/zip", intent.type)
        assertEquals(uri, intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
    }

    @Test
    fun `buildShareIntent sets FLAG_GRANT_READ_URI_PERMISSION`() {
        val intent = ShareIntentBuilder.buildShareIntent(uri)

        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun `buildShareIntent defaults to application slash zip`() {
        val intent = ShareIntentBuilder.buildShareIntent(uri)

        assertEquals("application/zip", intent.type)
    }

    @Test
    fun `buildPreviewIntent returns ACTION_VIEW with data and type`() {
        val pngUri = Uri.parse("content://com.androidvisualqa.app.fileprovider/exports/screenshot.png")
        val intent = ShareIntentBuilder.buildPreviewIntent(pngUri, "image/png")

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("image/png", intent.type)
        assertEquals(pngUri, intent.data)
    }

    @Test
    fun `buildPreviewIntent sets FLAG_GRANT_READ_URI_PERMISSION and NEW_TASK`() {
        val intent = ShareIntentBuilder.buildPreviewIntent(uri, "application/zip")

        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `buildChooserIntent wraps in ACTION_CHOOSER`() {
        val shareIntent = ShareIntentBuilder.buildShareIntent(uri)
        val chooser = ShareIntentBuilder.buildChooserIntent(shareIntent, "Share report")

        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
    }
}
