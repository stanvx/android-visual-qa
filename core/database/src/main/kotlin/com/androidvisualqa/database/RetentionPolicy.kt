package com.androidvisualqa.database

/**
 * Policy governing automatic deletion of old reports.
 *
 * Deletion priority order (first match wins):
 * 1. If [minFreeBytes] is set and free disk space falls below the threshold,
 *    delete the oldest reports until space is recovered.
 * 2. If [maxDays] is set and a report is older than that, delete it.
 * 3. If [maxDrafts] is set and the total count exceeds the limit, delete the
 *    oldest reports from the tail until the limit is satisfied.
 *
 * Set a field to [Int.MAX_VALUE] or [Long.MAX_VALUE] to effectively disable it.
 *
 * @property maxDrafts Maximum number of reports to retain. Default 200.
 * @property maxDays Maximum age in days for a retained report. Default 30.
 * @property minFreeBytes Minimum free disk bytes required. Default 500 MB.
 */
data class RetentionPolicy(
    val maxDrafts: Int = 200,
    val maxDays: Int = 30,
    val minFreeBytes: Long = 500L * 1024 * 1024,
) {
    /**
     * Evaluates whether [candidate] should be deleted given the current store state.
     *
     * @param candidate The report to evaluate.
     * @param totalCount Total number of reports currently stored.
     * @param oldestCreatedAt Epoch-millis timestamp of the oldest report in the store.
     * @param freeBytes Current free disk space in bytes.
     */
    fun shouldDelete(
        candidate: ReportEntity,
        totalCount: Int,
        oldestCreatedAt: Long,
        freeBytes: Long,
    ): Boolean {
        // 1. Free space
        if (freeBytes < minFreeBytes) return true

        // 2. Age limit — delete if older than maxDays
        val ageCutoff = System.currentTimeMillis() - (maxDays.toLong() * 86_400_000)
        if (candidate.createdAt < ageCutoff) return true

        // 3. Count limit — delete the oldest if we exceed
        if (totalCount > maxDrafts && candidate.createdAt == oldestCreatedAt) return true

        return false
    }
}
