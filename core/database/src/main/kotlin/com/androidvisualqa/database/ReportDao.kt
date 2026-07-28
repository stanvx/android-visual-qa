package com.androidvisualqa.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReportEntity)

    @Query("SELECT * FROM reports ORDER BY createdAt DESC LIMIT :limit")
    fun listRecent(limit: Int): Flow<List<ReportEntity>>

    /** Suspend variant for non-UI workers that don't need reactive emissions. */
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    suspend fun listAll(): List<ReportEntity>

    @Query("SELECT * FROM reports WHERE reportId = :id")
    suspend fun get(id: String): ReportEntity?

    @Query("SELECT COUNT(*) FROM reports")
    suspend fun count(): Int

    @Query("DELETE FROM reports WHERE createdAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int

    @Query("DELETE FROM reports WHERE reportId = :id")
    suspend fun deleteById(id: String)
}
