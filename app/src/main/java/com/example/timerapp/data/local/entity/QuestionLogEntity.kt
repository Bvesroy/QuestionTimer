package com.example.timerapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Per-question timing log belonging to a [SessionEntity].
 *
 * @property id            Surrogate primary key.
 * @property sessionId     FK to sessions.id (CASCADE on delete).
 * @property questionNumber 1-based question index within the session.
 * @property durationMs    Time spent on this question in milliseconds.
 * @property timestamp     Epoch millis when the question was answered (or logged).
 */
@Entity(
    tableName = "question_logs",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionId", "questionNumber"], unique = true)
    ]
)
data class QuestionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val questionNumber: Int,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)
