package com.example.timerapp.data.local.db

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Example type converters. Status is stored as a String directly (enum name),
 * so no converter is required for it; this class demonstrates Instant <-> Long.
 */
class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
}
