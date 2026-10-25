package com.example.timerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.timerapp.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert
    suspend fun insert(entry: HistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(entries: List<HistoryEntity>)

    @Delete
    suspend fun delete(entry: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clear()

    @Query("SELECT * FROM history ORDER BY completedAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY completedAt DESC")
    suspend fun getAll(): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE sessionId = :sessionId")
    suspend fun getBySession(sessionId: Long): List<HistoryEntity>
}
