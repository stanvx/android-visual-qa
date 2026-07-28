package com.androidvisualqa.app

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.androidvisualqa.database.RetentionConfig
import com.androidvisualqa.database.RetentionWorker
import java.util.concurrent.TimeUnit

/**
 * Schedules daily retention cleanup via WorkManager.
 *
 * Called from [MainActivity.onCreate] after the database is created.
 * The work runs once per day and enforces the configured [RetentionPolicy].
 *
 * @param context Application context for WorkManager access.
 */
public class RetentionScheduler(private val context: Context) {

    /**
     * Enqueues a periodic retention work request.
     *
     * Uses [ExistingPeriodicWorkPolicy.KEEP] so the work is not re-scheduled
     * on every app launch after the first.
     *
     * @param config Retention configuration. Defaults to [RetentionConfig] with
     *   the default work name.
     */
    public fun schedule(config: RetentionConfig = RetentionConfig()) {
        val request = PeriodicWorkRequestBuilder<RetentionWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            config.workName,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * Cancels the scheduled retention work.
     *
     * @param config Retention configuration matching the scheduled work.
     */
    public fun cancel(config: RetentionConfig = RetentionConfig()) {
        WorkManager.getInstance(context).cancelUniqueWork(config.workName)
    }
}
