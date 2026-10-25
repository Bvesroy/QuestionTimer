package com.example.timerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.timerapp.data.local.entity.QuestionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionLogDao {

    @Insert
    suspend fun insert(log: QuestionLogEntity): Long

    @Insert
    suspend fun insertAll(logs: List<QuestionLogEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(logs: List<QuestionLogEntity>)

    @Query("SELECT * FROM question_logs WHERE sessionId = :sessionId ORDER BY questionNumber ASC")
    fun observeBySession(sessionId: Long): Flow<List<QuestionLogEntity>>

    @Query("SELECT * FROM question_logs WHERE sessionId = :sessionId ORDER BY questionNumber ASC")
    suspend fun getBySession(sessionId: Long): List<QuestionLogEntity>

    @Query("SELECT * FROM question_logs")
    suspend fun getAll(): List<QuestionLogEntity>

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM question_logs WHERE sessionId = :sessionId")
    suspend fun totalDuration(sessionId: Long): Long

    @Query("SELECT COUNT(*) FROM question_logs WHERE sessionId = :sessionId")
    suspend fun count(sessionId: Long): Int

    @Query("DELETE FROM question_logs WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)

    @Query("DELETE FROM question_logs")
    suspend fun clear()
}
