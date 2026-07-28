package com.androidvisualqa.app

import com.androidvisualqa.database.RetentionConfig
import com.androidvisualqa.database.RetentionPolicy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Pure-JVM unit tests for [RetentionScheduler].
 *
 * Tests validate the [RetentionConfig] and [RetentionPolicy] parameters.
 * WorkManager integration is tested via `:core:database:RetentionWorkerTest`.
 *
 * ponytail: pure-JVM; skips WorkManager test infrastructure which requires
 * Android Context. Add WorkManager-specific integration tests when Robolectric
 * is already in the test dependency graph.
 */
class RetentionSchedulerTest {

    @Test
    fun `default config uses expected defaults`() {
        val config = RetentionConfig()
        assertEquals("visual-qa-retention", config.workName)
    }

    @Test
    fun `custom config applies overrides`() {
        val config = RetentionConfig(
            workName = "custom-test-retention",
        )
        assertEquals("custom-test-retention", config.workName)
    }

    @Test
    fun `shouldDelete respects maxDays`() {
        val policy = RetentionPolicy(maxDrafts = Int.MAX_VALUE, maxDays = 30)
        val oldReport = com.androidvisualqa.database.ReportEntity(
            reportId = "old",
            createdAt = System.currentTimeMillis() - 31L * 86_400_000,
            status = "Saved",
            packageName = "com.test",
            lastStateName = "Complete",
            schemaVersion = 1,
            reportJsonPath = "",
            annotatedPngPath = "",
            sha256 = "",
        )
        val recentReport = com.androidvisualqa.database.ReportEntity(
            reportId = "recent",
            createdAt = System.currentTimeMillis() - 1L * 86_400_000,
            status = "Saved",
            packageName = "com.test",
            lastStateName = "Complete",
            schemaVersion = 1,
            reportJsonPath = "",
            annotatedPngPath = "",
            sha256 = "",
        )
        // Disable count-based deletion by setting maxDrafts high enough
        assertEquals(true, policy.shouldDelete(oldReport, 2, oldReport.createdAt))
        assertEquals(false, policy.shouldDelete(recentReport, 2, oldReport.createdAt))
    }
}
