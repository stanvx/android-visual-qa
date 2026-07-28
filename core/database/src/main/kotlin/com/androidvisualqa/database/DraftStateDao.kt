package com.androidvisualqa.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DraftStateEntity)

    @Query("SELECT * FROM draft_states WHERE draftId = :id")
    suspend fun get(id: String): DraftStateEntity?

    @Query("DELETE FROM draft_states WHERE draftId = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM draft_states ORDER BY updatedAt DESC")
    fun listAll(): Flow<List<DraftStateEntity>>
}
