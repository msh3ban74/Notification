package com.notification.app.domain.calculator

import java.util.Calendar

/**
 * Final Product sprint (Phase C) — pure functions over habit logs.
 *
 * All computations key on "dayStart": epoch millis normalized to local
 * midnight. A day with a log row is completed; a day without one is
 * missed. No state is stored — streaks/percentages are derived, so the
 * daily reset is automatic and retro-toggling a day self-corrects
 * everything.
 */
object HabitCalculator {

    const val DAY_MS: Long = 24L * 60 * 60 * 1000

    /** Local midnight of the day containing [timeMillis]. */
    fun dayStartOf(timeMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Local midnight [days] days before/after [dayStart] (DST-safe). */
    fun shiftDay(dayStart: Long, days: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dayStart
        cal.add(Calendar.DAY_OF_YEAR, days)
        return dayStartOf(cal.timeInMillis)
    }

    /**
     * Consecutive completed days ending today (or yesterday — an
     * unfinished today never breaks a live streak, it just doesn't
     * count yet).
     */
    fun currentStreak(days: Set<Long>, today: Long = dayStartOf()): Int {
        var cursor = if (today in days) today else shiftDay(today, -1)
        var streak = 0
        while (cursor in days) {
            streak++
            cursor = shiftDay(cursor, -1)
        }
        return streak
    }

    /** Longest run of consecutive completed days ever. */
    fun longestStreak(days: Set<Long>): Int {
        if (days.isEmpty()) return 0
        var longest = 0
        for (day in days) {
            // Only count from the start of each run.
            if (shiftDay(day, -1) in days) continue
            var length = 1
            var cursor = shiftDay(day, 1)
            while (cursor in days) {
                length++
                cursor = shiftDay(cursor, 1)
            }
            if (length > longest) longest = length
        }
        return longest
    }

    /** Completed days within the last [windowDays] days (today inclusive). */
    fun completionsInWindow(days: Set<Long>, windowDays: Int, today: Long = dayStartOf()): Int {
        val from = shiftDay(today, -(windowDays - 1))
        return days.count { it in from..today }
    }

    /** 0..100 completion percent over the last [windowDays] days. */
    fun completionPercent(days: Set<Long>, windowDays: Int, today: Long = dayStartOf()): Int {
        if (windowDays <= 0) return 0
        return (completionsInWindow(days, windowDays, today) * 100) / windowDays
    }

    /**
     * The dayStarts of every day in the month containing [anyDayInMonth],
     * in order — drives the mini calendar grid.
     */
    fun daysOfMonth(anyDayInMonth: Long = dayStartOf()): List<Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dayStartOf(anyDayInMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val first = cal.timeInMillis
        val count = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (0 until count).map { shiftDay(first, it) }
    }
}
