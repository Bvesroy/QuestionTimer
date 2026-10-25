package com.example.timerapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Lifecycle status of a timer session.
 * Stored as the enum name (String) in the database.
 */
enum class SessionStatus {
    CREATED,
    RUNNING,
    PAUSED,
    COMPLETED,
    ABANDONED,
    FAILED
}

/**
 * A timer session. One row per session started by the user.
 *
 * @property id            Surrogate primary key (auto-generated).
 * @property name          User-facing session name.
 * @property target        Target number of questions to complete.
 * @property currentQuestion Index (1-based) of the question currently being answered; 0 = none started.
 * @property status        [SessionStatus] name.
 * @property tags          Free-form tags (comma separated) added in schema v2.
 * @property createdAt     Epoch millis when the session was created.
 * @property updatedAt     Epoch millis of last mutation.
 */
@Entity(
    tableName = "sessions",
    indices = [Index(value = ["status"]), Index(value = ["createdAt"])]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val target: Int,
    val currentQuestion: Int = 0,
    val status: String = SessionStatus.CREATED.name,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
