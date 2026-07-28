package com.androidvisualqa.database

/**
 * Configuration holder so [app] can override retention defaults without
 * editing the database module.
 *
 * @property policy The retention policy to apply.
 * @property workName Unique WorkManager periodic work name.
 */
data class RetentionConfig(
    val policy: RetentionPolicy = RetentionPolicy(),
    val workName: String = "visual-qa-retention",
)
