package com.androidvisualqa.database

/**
 * Policy governing automatic deletion of old reports.
 *
 * Deletion priority order:
 * 1. If [maxDays] is set and a report is older than that, delete it.
 * 2. If [maxDrafts] is set and the total count exceeds the limit, delete the
 *    oldest reports from the tail until the limit is satisfied.
 *
 * Free-space-based deletion is handled by [RetentionWorker] independently —
 * see `low-disk guard` in [RetentionWorker.deleteExcessReports].
 *
 * Set a field to [Int.MAX_VALUE] to effectively disable it.
 *
 * @property maxDrafts Maximum number of reports to retain. Default 200.
 * @property maxDays Maximum age in days for a retained report. Default 30.
 */
data class RetentionPolicy(
    val maxDrafts: Int = 200,
    val maxDays: Int = 30,
) {
    /**
     * Evaluates whether [candidate] should be deleted given the current store state.
     *
     * @param candidate The report to evaluate.
     * @param totalCount Total number of reports currently stored.
     * @param oldestCreatedAt Epoch-millis timestamp of the oldest report in the store.
     */
    fun shouldDelete(
        candidate: ReportEntity,
        totalCount: Int,
        oldestCreatedAt: Long,
    ): Boolean {
        // 1. Age limit — delete if older than maxDays
        val ageCutoff = System.currentTimeMillis() - (maxDays.toLong() * 86_400_000)
        if (candidate.createdAt < ageCutoff) return true

        // 2. Count limit — delete the oldest if we exceed
        if (totalCount > maxDrafts && candidate.createdAt == oldestCreatedAt) return true

        return false
    }
}
