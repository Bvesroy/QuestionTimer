package com.example.timerapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Historical summary of a finished (completed/abandoned/failed) session.
 * Denormalised so history survives deletion of the live session row.
 */
@Entity(
    tableName = "history",
    indices = [Index(value = ["completedAt"]), Index(value = ["sessionId"])]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long? = null,
    val sessionName: String,
    val target: Int,
    val questionsCompleted: Int,
    val totalDurationMs: Long,
    val completedAt: Long = System.currentTimeMillis(),
    val status: String
)
