package com.androidvisualqa.database

/**
 * Configuration holder so [app] can override retention defaults without
 * editing the database module.
 *
 * @property workName Unique WorkManager periodic work name.
 *
 * // TODO(m4): add policy config when `:app` exposes user-controlled retention settings
 */
data class RetentionConfig(
    val workName: String = "visual-qa-retention",
)
