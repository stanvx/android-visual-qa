package com.androidvisualqa.database

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetentionPolicyTest {

    private val now = System.currentTimeMillis()
    private val day = 86_400_000L

    @Test
    fun `shouldDelete returns true when count exceeds maxDrafts and candidate is oldest`() {
        val policy = RetentionPolicy(maxDrafts = 3, maxDays = 365, minFreeBytes = 0)
        val oldest = ReportEntity(
            reportId = "r1",
            createdAt = now - 10,
            status = "Saved",
            packageName = "com.example",
            lastStateName = "Complete",
            schemaVersion = 1,
            reportJsonPath = "drafts/r1/report.json",
            annotatedPngPath = "drafts/r1/annotated.png",
            sha256 = "aaa",
        )

        // totalCount > maxDrafts and candidate is the oldest
        assertTrue(policy.shouldDelete(oldest, totalCount = 5, oldestCreatedAt = now - 10, freeBytes = Long.MAX_VALUE))
    }

    @Test
    fun `shouldDelete returns false when under maxDrafts threshold`() {
        val policy = RetentionPolicy(maxDrafts = 10, maxDays = 365, minFreeBytes = 0)
        val candidate = ReportEntity(
            reportId = "r1",
            createdAt = now,
            status = "Saved",
            packageName = "com.example",
            lastStateName = "Complete",
            schemaVersion = 1,
            reportJsonPath = "drafts/r1/report.json",
            annotatedPngPath = "drafts/r1/annotated.png",
            sha256 = "aaa",
        )

        // totalCount of 3 < maxDrafts of 10
        assertFalse(policy.shouldDelete(candidate, totalCount = 3, oldestCreatedAt = now, freeBytes = Long.MAX_VALUE))
    }

    @Test
    fun `shouldDelete returns true when candidate is older than maxDays`() {
        val policy = RetentionPolicy(maxDrafts = Int.MAX_VALUE, maxDays = 7, minFreeBytes = 0)
        val oldReport = ReportEntity(
            reportId = "old",
            createdAt = now - (10 * day), // 10 days old
            status = "Saved",
            packageName = "com.example",
            lastStateName = "Complete",
            schemaVersion = 1,
            reportJsonPath = "drafts/old/report.json",
            annotatedPngPath = "drafts/old/annotated.png",
            sha256 = "bbb",
        )

        assertTrue(policy.shouldDelete(oldReport, totalCount = 1, oldestCreatedAt = now - (10 * day), freeBytes = Long.MAX_VALUE))
    }

    @Test
    fun `shouldDelete returns false when candidate is within maxDays`() {
        val policy = RetentionPolicy(maxDrafts = Int.MAX_VALUE, maxDays = 7, minFreeBytes = 0)
        val recentReport = ReportEntity(
            reportId = "recent",
            createdAt = now - (1 * day), // 1 day old
            status = "Saved",
            packageName = "com.example",
            lastStateName = "Complete",
            schemaVersion = 1,
            reportJsonPath = "drafts/recent/report.json",
            annotatedPngPath = "drafts/recent/annotated.png",
            sha256 = "ccc",
        )

        assertFalse(policy.shouldDelete(recentReport, totalCount = 1, oldestCreatedAt = now - (1 * day), freeBytes = Long.MAX_VALUE))
    }

    @Test
    fun `shouldDelete returns true when free space is below minFreeBytes`() {
        val policy = RetentionPolicy(maxDrafts = Int.MAX_VALUE, maxDays = 365, minFreeBytes = 500 * 1024 * 1024)
        val candidate = ReportEntity(
            reportId = "r1",
            createdAt = now,
            status = "Saved",
            packageName = "com.example",
            lastStateName = "Complete",
            schemaVersion = 1,
            reportJsonPath = "drafts/r1/report.json",
            annotatedPngPath = "drafts/r1/annotated.png",
            sha256 = "ddd",
        )

        // Only 10 MB free — well below 500 MB threshold
        assertTrue(policy.shouldDelete(candidate, totalCount = 1, oldestCreatedAt = now, freeBytes = 10 * 1024 * 1024))
    }

    @Test
    fun `shouldDelete returns false when oldest candidate is not the only oldest`() {
        val policy = RetentionPolicy(maxDrafts = 3, maxDays = 365, minFreeBytes = 0)
        // This candidate is NOT the oldest — another report has a lower createdAt
        val candidate = ReportEntity(
            reportId = "r2",
            createdAt = now,
            status = "Saved",
            packageName = "com.example",
            lastStateName = "Complete",
            schemaVersion = 1,
            reportJsonPath = "drafts/r2/report.json",
            annotatedPngPath = "drafts/r2/annotated.png",
            sha256 = "eee",
        )

        // totalCount > maxDrafts (5 > 3), but this candidate is not the oldest (oldest is now - 100)
        assertFalse(policy.shouldDelete(candidate, totalCount = 5, oldestCreatedAt = now - 100, freeBytes = Long.MAX_VALUE))
    }
}
