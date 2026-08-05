package com.notification.app.domain.model

/**
 * Sprint 3 — Smart Task.
 *
 * Priority level for a Task created from the Smart Item flow.
 *
 * IMPORTANT: [ReminderEntity] has no dedicated priority column and this
 * sprint does NOT touch the Room schema. Instead, priority is expressed
 * through the reminder system that already exists: a higher priority
 * simply schedules MORE of the existing [PreAlertOption] pre-alerts.
 * No new scheduling implementation is introduced.
 */
enum class TaskPriority(
    val displayNameEn: String,
    val displayNameAr: String,
    val preAlerts: List<PreAlertOption>
) {
    LOW(
        "Low", "منخفضة",
        listOf(PreAlertOption.ONE_DAY)
    ),
    MEDIUM(
        "Medium", "متوسطة",
        listOf(PreAlertOption.ONE_DAY, PreAlertOption.ONE_HOUR)
    ),
    HIGH(
        "High", "عالية",
        listOf(PreAlertOption.ONE_DAY, PreAlertOption.ONE_HOUR, PreAlertOption.THIRTY_MINS)
    );

    /** Serialized exactly like [ReminderEntity.preAlerts] expects (comma-separated names). */
    fun toPreAlertsString(): String = preAlerts.joinToString(",") { it.name }
}
