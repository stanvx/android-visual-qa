package com.androidvisualqa.files

import com.androidvisualqa.model.ids.DraftId
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class DraftDirectoryTest {

    private val id = DraftId("test-draft-001")
    private val dir = DraftDirectory(Path.of("/tmp/vqa"))

    @Test
    fun `all path helpers are under draft root`() {
        val draftRoot = dir.draftPath(id)
        assertTrue(dir.manifestPath(id).startsWith(draftRoot))
        assertTrue(dir.originalImagePath(id).startsWith(draftRoot))
        assertTrue(dir.annotatedImagePath(id).startsWith(draftRoot))
        assertTrue(dir.reportJsonPath(id).startsWith(draftRoot))
        assertTrue(dir.reportMarkdownPath(id).startsWith(draftRoot))
        assertTrue(dir.attachmentPath(id, "voice.m4a").startsWith(draftRoot))
    }

    @Test
    fun `draftPath contains the id value`(@TempDir tempDir: Path) {
        val localDir = DraftDirectory(tempDir)
        val p = localDir.draftPath(id)
        assertTrue(p.toString().contains("test-draft-001"))
    }

    @Test
    fun `attachmentPath includes subdirectory and filename`() {
        val p = dir.attachmentPath(id, "voice.m4a")
        assertTrue(p.toString().endsWith("voice.m4a"))
        assertTrue(p.toString().contains("attachments"))
    }
}
