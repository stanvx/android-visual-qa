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
        assertEquals(200, config.policy.maxDrafts)
        assertEquals(30, config.policy.maxDays)
        assertEquals(500L * 1024 * 1024, config.policy.minFreeBytes)
    }

    @Test
    fun `custom config applies overrides`() {
        val config = RetentionConfig(
            policy = RetentionPolicy(maxDrafts = 50, maxDays = 7, minFreeBytes = 100L * 1024 * 1024),
            workName = "custom-test-retention",
        )
        assertEquals("custom-test-retention", config.workName)
        assertEquals(50, config.policy.maxDrafts)
        assertEquals(7, config.policy.maxDays)
        assertEquals(100L * 1024 * 1024, config.policy.minFreeBytes)
    }

    @Test
    fun `shouldDelete respects maxDays`() {
        val policy = RetentionPolicy(maxDrafts = Int.MAX_VALUE, maxDays = 30, minFreeBytes = 0)
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
        assertEquals(true, policy.shouldDelete(oldReport, 2, oldReport.createdAt, Long.MAX_VALUE))
        assertEquals(false, policy.shouldDelete(recentReport, 2, oldReport.createdAt, Long.MAX_VALUE))
    }
}
