package com.androidvisualqa.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ReportEntity::class, DraftStateEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ReportDatabase : RoomDatabase() {
    abstract fun reports(): ReportDao
    abstract fun draftStateDao(): DraftStateDao

    companion object {
        private const val DB_NAME = "visualqa-reports.db"

        fun build(context: Context): ReportDatabase =
            Room.databaseBuilder(context, ReportDatabase::class.java, DB_NAME)
                .addMigrations() // TODO(m4): add real migrations when schema bumps
                .build()

        /** In-memory instance for testing — data is lost when the process ends. */
        fun inMemory(context: Context): ReportDatabase =
            Room.inMemoryDatabaseBuilder(context, ReportDatabase::class.java)
                .addMigrations() // TODO(m4): add real migrations when schema bumps
                .build()
    }
}
