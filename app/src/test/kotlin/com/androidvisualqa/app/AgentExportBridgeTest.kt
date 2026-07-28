package com.androidvisualqa.app

import com.androidvisualqa.app.export.AgentExportBridge
import com.androidvisualqa.export.agent.AgentBundleBuilder
import com.androidvisualqa.export.share.FileProviderWriter
import com.androidvisualqa.model.ids.ReportId
import com.androidvisualqa.testing.testReport
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * Unit tests for [AgentExportBridge].
 *
 * Verifies that the agent bundle JSON is written to a FileProvider-visible
 * location before the FileProvider URI resolution (which throws in
 * [FakeContext]).
 *
 * ponytail: validates the cache file content rather than the URI, since
 * FileProvider resolution requires platform mocking.
 */
class AgentExportBridgeTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var context: FakeContext
    private lateinit var bridge: AgentExportBridge
    private lateinit var report: com.androidvisualqa.model.VisualFeedbackReport

    @BeforeEach
    fun setUp() {
        context = FakeContext(tempDir.toFile())
        val bundleBuilder = AgentBundleBuilder(
            canvasWidth = 1080,
            canvasHeight = 2400,
        )
        val fileProviderWriter = FileProviderWriter(context)
        bridge = AgentExportBridge(bundleBuilder, fileProviderWriter)
        report = testReport(reportId = ReportId("test-agent-001"))
    }

    @Test
    fun `exportAgentBundle writes JSON cache file and returns failure`() = runBlocking {
        // FileProviderWriter.writeBytesToCache catches NotImplementedError
        // in its own runCatching, so bridge returns a Result.failure directly.
        val result = bridge.exportAgentBundle(report)

        // The cache file should be written before FileProvider URI resolution
        val cacheFile = File(context.cacheDir, "agent-exports/test-agent-001.json")
        assertTrue(cacheFile.exists()) {
            "Cache file should exist at ${cacheFile.absolutePath}"
        }
        assertTrue(cacheFile.length() > 0) {
            "JSON file should be non-empty, got size ${cacheFile.length()}"
        }
        val content = cacheFile.readText()
        assertTrue(content.contains("test-agent-001")) {
            "JSON content should contain the report ID"
        }

        // URI resolution throws NotImplementedError caught by runCatching
        assertTrue(result.isFailure) {
            "exportAgentBundle must fail in unit test without platform mocking"
        }
    }

    @Test
    fun `shareAgentBundle writes cache file and returns failure`() = runBlocking {
        val result = bridge.shareAgentBundle(report)

        // The cache file should be written regardless
        val cacheFile = File(context.cacheDir, "agent-exports/test-agent-001.json")
        assertTrue(cacheFile.exists()) {
            "Cache file should exist before share intent is built"
        }

        assertTrue(result.isFailure) {
            "shareAgentBundle must fail in unit test without platform mocking"
        }
        assertNotNull(result.exceptionOrNull())
    }
}
