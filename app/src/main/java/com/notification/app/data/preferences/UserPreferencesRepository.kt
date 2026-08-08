package com.notification.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferenceKeys {
        val LANGUAGE = stringPreferencesKey("app_language") // "ar" or "en"
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val USER_NAME = stringPreferencesKey("user_display_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val WATER_INTERVAL = intPreferencesKey("water_interval_hours")
        val PRAYER_FAJR = booleanPreferencesKey("prayer_fajr")
        val PRAYER_DHUHR = booleanPreferencesKey("prayer_dhuhr")
        val PRAYER_ASR = booleanPreferencesKey("prayer_asr")
        val PRAYER_MAGHRIB = booleanPreferencesKey("prayer_maghrib")
        val PRAYER_ISHA = booleanPreferencesKey("prayer_isha")
        val ADHKAR_MORNING = booleanPreferencesKey("adhkar_morning")
        val ADHKAR_EVENING = booleanPreferencesKey("adhkar_evening")
        val QIYAM_ENABLED = booleanPreferencesKey("qiyam_enabled")
        val DUHA_ENABLED = booleanPreferencesKey("duha_enabled")
        val ALARM_RINGTONE_URI = stringPreferencesKey("default_alarm_ringtone_uri")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_timestamp")
        // AI sprint — the visible conversation, persisted as JSON so it
        // survives process death / app restart (ViewModel alone only
        // survives rotation).
        val CHAT_HISTORY = stringPreferencesKey("ai_chat_history_json")
        // Water tracker — persisted per-day so the count survives app
        // restart / process death and resets automatically on a new day.
        val WATER_COUNT = intPreferencesKey("water_count_today")
        val WATER_DAY = longPreferencesKey("water_count_day_start")
        // Automatic cloud backup — a single ON/OFF switch. When on, the app
        // silently mirrors every data change to the cloud.
        val AUTO_CLOUD_BACKUP = booleanPreferencesKey("auto_cloud_backup_enabled")
        val AUTO_BACKUP_LAST = longPreferencesKey("auto_backup_last_at")
        // Adhkar / nafl daily reminders — each a persisted ON/OFF switch that
        // schedules (or cancels) a gentle daily notification.
        val ADHKAR_MORNING = booleanPreferencesKey("adhkar_morning_enabled")
        val ADHKAR_EVENING = booleanPreferencesKey("adhkar_evening_enabled")
        val ADHKAR_QIYAM = booleanPreferencesKey("adhkar_qiyam_enabled")
        val ADHKAR_DUHA = booleanPreferencesKey("adhkar_duha_enabled")
    }

    private fun todayStartMillis(): Long = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Today's water count; automatically 0 once the stored day is not today. */
    val waterCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        if ((prefs[PreferenceKeys.WATER_DAY] ?: 0L) == todayStartMillis()) prefs[PreferenceKeys.WATER_COUNT] ?: 0 else 0
    }

    suspend fun setWaterCount(count: Int) {
        context.dataStore.edit {
            it[PreferenceKeys.WATER_COUNT] = count.coerceAtLeast(0)
            it[PreferenceKeys.WATER_DAY] = todayStartMillis()
        }
    }

    val autoCloudBackupFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferenceKeys.AUTO_CLOUD_BACKUP] ?: false }
    val autoBackupLastFlow: Flow<Long> = context.dataStore.data.map { it[PreferenceKeys.AUTO_BACKUP_LAST] ?: 0L }

    suspend fun setAutoCloudBackup(v: Boolean) { context.dataStore.edit { it[PreferenceKeys.AUTO_CLOUD_BACKUP] = v } }
    suspend fun setAutoBackupLast(v: Long) { context.dataStore.edit { it[PreferenceKeys.AUTO_BACKUP_LAST] = v } }

    val adhkarMorningFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferenceKeys.ADHKAR_MORNING] ?: false }
    val adhkarEveningFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferenceKeys.ADHKAR_EVENING] ?: false }
    val adhkarQiyamFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferenceKeys.ADHKAR_QIYAM] ?: false }
    val adhkarDuhaFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferenceKeys.ADHKAR_DUHA] ?: false }

    suspend fun setAdhkarMorning(v: Boolean) { context.dataStore.edit { it[PreferenceKeys.ADHKAR_MORNING] = v } }
    suspend fun setAdhkarEvening(v: Boolean) { context.dataStore.edit { it[PreferenceKeys.ADHKAR_EVENING] = v } }
    suspend fun setAdhkarQiyam(v: Boolean) { context.dataStore.edit { it[PreferenceKeys.ADHKAR_QIYAM] = v } }
    suspend fun setAdhkarDuha(v: Boolean) { context.dataStore.edit { it[PreferenceKeys.ADHKAR_DUHA] = v } }

    val chatHistoryFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferenceKeys.CHAT_HISTORY] ?: ""
    }

    suspend fun setChatHistory(json: String) {
        context.dataStore.edit { prefs -> prefs[PreferenceKeys.CHAT_HISTORY] = json }
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferenceKeys.LANGUAGE] ?: "ar" // Default to Arabic RTL as per brief
    }

    val isDarkModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferenceKeys.DARK_MODE] ?: true
    }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferenceKeys.IS_LOGGED_IN] ?: false
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferenceKeys.USER_NAME] ?: "Guest User"
    }

    val userEmailFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferenceKeys.USER_EMAIL] ?: ""
    }

    val waterIntervalFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferenceKeys.WATER_INTERVAL] ?: 2
    }

    val prayerSettingsFlow: Flow<Map<String, Boolean>> = context.dataStore.data.map { prefs ->
        mapOf(
            "fajr" to (prefs[PreferenceKeys.PRAYER_FAJR] ?: true),
            "dhuhr" to (prefs[PreferenceKeys.PRAYER_DHUHR] ?: true),
            "asr" to (prefs[PreferenceKeys.PRAYER_ASR] ?: true),
            "maghrib" to (prefs[PreferenceKeys.PRAYER_MAGHRIB] ?: true),
            "isha" to (prefs[PreferenceKeys.PRAYER_ISHA] ?: true),
            "morning" to (prefs[PreferenceKeys.ADHKAR_MORNING] ?: true),
            "evening" to (prefs[PreferenceKeys.ADHKAR_EVENING] ?: true),
            "qiyam" to (prefs[PreferenceKeys.QIYAM_ENABLED] ?: true),
            "duha" to (prefs[PreferenceKeys.DUHA_ENABLED] ?: true)
        )
    }

    val alarmRingtoneUriFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferenceKeys.ALARM_RINGTONE_URI] ?: ""
    }

    val lastSyncTimeFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[PreferenceKeys.LAST_SYNC_TIME] ?: 0L
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { prefs -> prefs[PreferenceKeys.LANGUAGE] = lang }
    }

    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferenceKeys.DARK_MODE] = isDark }
    }

    suspend fun setUserAuth(name: String, email: String, isLoggedIn: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.USER_NAME] = name
            prefs[PreferenceKeys.USER_EMAIL] = email
            prefs[PreferenceKeys.IS_LOGGED_IN] = isLoggedIn
        }
    }

    suspend fun setWaterInterval(hours: Int) {
        context.dataStore.edit { prefs -> prefs[PreferenceKeys.WATER_INTERVAL] = hours }
    }

    suspend fun setPrayerToggle(key: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            when (key) {
                "fajr" -> prefs[PreferenceKeys.PRAYER_FAJR] = enabled
                "dhuhr" -> prefs[PreferenceKeys.PRAYER_DHUHR] = enabled
                "asr" -> prefs[PreferenceKeys.PRAYER_ASR] = enabled
                "maghrib" -> prefs[PreferenceKeys.PRAYER_MAGHRIB] = enabled
                "isha" -> prefs[PreferenceKeys.PRAYER_ISHA] = enabled
                "morning" -> prefs[PreferenceKeys.ADHKAR_MORNING] = enabled
                "evening" -> prefs[PreferenceKeys.ADHKAR_EVENING] = enabled
                "qiyam" -> prefs[PreferenceKeys.QIYAM_ENABLED] = enabled
                "duha" -> prefs[PreferenceKeys.DUHA_ENABLED] = enabled
            }
        }
    }

    suspend fun setAlarmRingtoneUri(uri: String) {
        context.dataStore.edit { prefs -> prefs[PreferenceKeys.ALARM_RINGTONE_URI] = uri }
    }

    suspend fun updateLastSyncTime(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs -> prefs[PreferenceKeys.LAST_SYNC_TIME] = timestamp }
    }
}
