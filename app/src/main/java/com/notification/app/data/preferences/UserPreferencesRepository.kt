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
        val GEMINI_KEY = stringPreferencesKey("gemini_api_key")
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
        // Sprint 3 — automatic scheduled backup settings.
        val AUTO_BACKUP_FREQ = stringPreferencesKey("auto_backup_frequency") // OFF/DAILY/WEEKLY/MONTHLY
        val AUTO_BACKUP_CHARGING = booleanPreferencesKey("auto_backup_charging_only")
        val AUTO_BACKUP_WIFI = booleanPreferencesKey("auto_backup_wifi_only")
        val AUTO_BACKUP_TREE_URI = stringPreferencesKey("auto_backup_tree_uri")
        val AUTO_BACKUP_LAST = longPreferencesKey("auto_backup_last_at")
    }

    val autoBackupFreqFlow: Flow<String> = context.dataStore.data.map { it[PreferenceKeys.AUTO_BACKUP_FREQ] ?: "OFF" }
    val autoBackupChargingFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferenceKeys.AUTO_BACKUP_CHARGING] ?: false }
    val autoBackupWifiFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferenceKeys.AUTO_BACKUP_WIFI] ?: false }
    val autoBackupTreeUriFlow: Flow<String> = context.dataStore.data.map { it[PreferenceKeys.AUTO_BACKUP_TREE_URI] ?: "" }
    val autoBackupLastFlow: Flow<Long> = context.dataStore.data.map { it[PreferenceKeys.AUTO_BACKUP_LAST] ?: 0L }

    suspend fun setAutoBackupFreq(v: String) { context.dataStore.edit { it[PreferenceKeys.AUTO_BACKUP_FREQ] = v } }
    suspend fun setAutoBackupCharging(v: Boolean) { context.dataStore.edit { it[PreferenceKeys.AUTO_BACKUP_CHARGING] = v } }
    suspend fun setAutoBackupWifi(v: Boolean) { context.dataStore.edit { it[PreferenceKeys.AUTO_BACKUP_WIFI] = v } }
    suspend fun setAutoBackupTreeUri(v: String) { context.dataStore.edit { it[PreferenceKeys.AUTO_BACKUP_TREE_URI] = v } }
    suspend fun setAutoBackupLast(v: Long) { context.dataStore.edit { it[PreferenceKeys.AUTO_BACKUP_LAST] = v } }

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

    val geminiApiKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferenceKeys.GEMINI_KEY] ?: ""
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

    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { prefs -> prefs[PreferenceKeys.GEMINI_KEY] = key }
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
