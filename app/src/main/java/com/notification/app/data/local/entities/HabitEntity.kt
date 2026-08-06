package com.notification.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Final Product sprint (Phase C) — the professional habit engine.
 *
 * A habit is completed per-day: each completion is one [HabitLogEntity]
 * row keyed by the habit and the DAY (epoch millis normalized to local
 * midnight). "Daily reset" is therefore automatic — a new day simply has
 * no log row yet, and a day that ended without a row counts as missed.
 * Streaks, calendars and percentages are pure functions over the logs
 * (see HabitCalculator). Added via Room Migration 3→4 (CREATE TABLE only).
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val emoji: String = "✅",
    val note: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "habit_logs",
    indices = [Index(value = ["habitId", "dayStart"], unique = true)]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    /** Local midnight of the completed day (epoch millis). */
    val dayStart: Long,
    val completedAt: Long = System.currentTimeMillis()
)
