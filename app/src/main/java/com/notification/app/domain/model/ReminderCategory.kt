package com.notification.app.domain.model

enum class ReminderCategory(val displayNameEn: String, val displayNameAr: String, val iconName: String) {
    MONEY("Money & Installments", "الأقساط والديون", "attach_money"),
    APPOINTMENT("Appointments", "المواعيد والزيارات", "event"),
    BIRTHDAY("Birthdays", "أعياد الميلاد", "cake"),
    BILL("Bills & Utilities", "الفواتير والخدمات", "receipt"),
    TUTORING("Tutoring & Lessons", "الدروس والتعليم", "school"),
    // Sprint 5 — Smart Medicine. Display metadata only (categories are
    // stored as plain strings, so no schema change): unknown values still
    // fall back to CUSTOM via fromString below.
    MEDICINE("Medicine & Health", "الدواء والصحة", "medication"),
    // Final Product — real categories for the remaining smart-item types
    // (stored as plain strings; additive and fully backward-compatible).
    WORK("Work & Projects", "الشغل والمشاريع", "work"),
    EVENT("Events & Occasions", "المناسبات والاحتفالات", "celebration"),
    PERSONAL("Personal", "أموري الشخصية", "person"),
    CUSTOM("Custom", "مخصص", "notifications");

    companion object {
        fun fromString(value: String): ReminderCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: CUSTOM
        }
    }
}

enum class RecurrenceType(val displayNameEn: String, val displayNameAr: String) {
    NONE("No Recurrence", "بدون تكرار"),
    DAILY("Daily", "يومياً"),
    WEEKLY("Weekly", "أسبوعياً"),
    MONTHLY("Monthly", "شهرياً"),
    YEARLY("Yearly", "سنوياً");

    companion object {
        fun fromString(value: String): RecurrenceType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}

enum class PreAlertOption(val minutesBefore: Long, val displayNameEn: String, val displayNameAr: String) {
    ONE_MONTH(30L * 24 * 60, "1 month before", "قبل شهر"),
    ONE_WEEK(7L * 24 * 60, "1 week before", "قبل أسبوع"),
    ONE_DAY(24L * 60, "1 day before", "قبل يوم"),
    ONE_HOUR(60L, "1 hour before", "قبل ساعة"),
    THIRTY_MINS(30L, "30 minutes before", "قبل 30 دقيقة")
}
