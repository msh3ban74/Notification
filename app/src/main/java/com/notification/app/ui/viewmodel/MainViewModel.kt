package com.notification.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notification.app.data.local.AppDatabase
import com.notification.app.data.local.entities.*
import com.notification.app.data.preferences.UserPreferencesRepository
import com.notification.app.data.remote.GeminiContent
import com.notification.app.data.repository.GeminiRepository
import com.notification.app.data.repository.NotificationRepository
import com.notification.app.domain.calculator.PrayerTime
import com.notification.app.domain.calculator.PrayerTimesCalculator
import com.notification.app.domain.model.AiSuggestion
import com.notification.app.domain.model.LedgerTransactionType
import com.notification.app.domain.model.ReminderCategory
import com.notification.app.domain.scheduler.AlarmManagerScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = NotificationRepository(db)
    val preferencesRepository = UserPreferencesRepository(application)
    private val geminiRepository = GeminiRepository(repository)

    // User Preferences State
    val language: StateFlow<String> = preferencesRepository.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ar")

    val isDarkMode: StateFlow<Boolean> = preferencesRepository.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isLoggedIn: StateFlow<Boolean> = preferencesRepository.isLoggedInFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userName: StateFlow<String> = preferencesRepository.userNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "User")

    val userEmail: StateFlow<String> = preferencesRepository.userEmailFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val geminiApiKey: StateFlow<String> = preferencesRepository.geminiApiKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val lastSyncTime: StateFlow<Long> = preferencesRepository.lastSyncTimeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val alarmRingtoneUri: StateFlow<String> = preferencesRepository.alarmRingtoneUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Database Flows
    val allReminders: StateFlow<List<ReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingReminders: StateFlow<List<ReminderEntity>> = repository.pendingReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPersons: StateFlow<List<PersonEntity>> = repository.allPersons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<LedgerTransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGam3iyas: StateFlow<List<Gam3iyaEntity>> = repository.allGam3iyas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlarms: StateFlow<List<AlarmEntity>> = repository.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWorkNotes: StateFlow<List<WorkNoteEntity>> = repository.allWorkNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Islamic Prayers State
    private val _prayerTimes = MutableStateFlow<List<PrayerTime>>(emptyList())
    val prayerTimes: StateFlow<List<PrayerTime>> = _prayerTimes.asStateFlow()

    // AI Chat Messages State
    private val _chatMessages = MutableStateFlow<List<GeminiContent>>(emptyList())
    val chatMessages: StateFlow<List<GeminiContent>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Dashboard "Rafeeq Suggestions" — produced by the EXISTING Gemini
    // pipeline from real data (see GeminiRepository.generateDashboardSuggestions).
    private val _aiSuggestions = MutableStateFlow<List<AiSuggestion>>(emptyList())
    val aiSuggestions: StateFlow<List<AiSuggestion>> = _aiSuggestions.asStateFlow()

    private val _aiSuggestionsLoading = MutableStateFlow(false)
    val aiSuggestionsLoading: StateFlow<Boolean> = _aiSuggestionsLoading.asStateFlow()

    /**
     * Refreshes the dashboard AI suggestions. Reuses the existing Gemini
     * repository and API-key resolution; on failure the list simply stays
     * empty and the dashboard falls back to its local rule-based insights.
     */
    fun refreshAiSuggestions(isArabic: Boolean, force: Boolean = false) {
        if (_aiSuggestionsLoading.value) return
        if (!force && _aiSuggestions.value.isNotEmpty()) return
        viewModelScope.launch {
            _aiSuggestionsLoading.value = true
            _aiSuggestions.value = geminiRepository.generateDashboardSuggestions(
                isArabic = isArabic,
                customApiKey = geminiApiKey.value
            )
            _aiSuggestionsLoading.value = false
        }
    }

    // Water counter state
    private val _waterCount = MutableStateFlow(0)
    val waterCount: StateFlow<Int> = _waterCount.asStateFlow()

    init {
        updatePrayerTimes()
    }

    fun updatePrayerTimes() {
        _prayerTimes.value = PrayerTimesCalculator.getDailyPrayerTimes(Date())
    }

    // Reminders Actions
    fun addReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            insertAndScheduleReminder(reminder)
        }
    }

    /**
     * The single insert-and-schedule path for every reminder in the app.
     * Sprint 3/4 reuse: called by [addReminder] (Tasks) and by [addDebt]
     * (due-date reminder), so there is exactly ONE scheduling pipeline.
     */
    private suspend fun insertAndScheduleReminder(reminder: ReminderEntity): Long {
        val id = repository.insertReminder(reminder)
        AlarmManagerScheduler.scheduleReminderAlarm(getApplication(), reminder.copy(id = id))
        return id
    }

    /**
     * Sprint 5 — edit flow for Tasks (and every other reminder-based Smart
     * Item). Persists through the existing repository and re-schedules via
     * the existing AlarmManagerScheduler: the reminder's PendingIntent
     * request code is derived from its id, so scheduling again simply
     * replaces the previously scheduled alarm.
     */
    fun updateTaskReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
            AlarmManagerScheduler.scheduleReminderAlarm(getApplication(), reminder)
        }
    }

    fun toggleReminderCompleted(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    // Ledger Actions
    fun addPerson(name: String, phone: String = "") {
        viewModelScope.launch {
            repository.insertPerson(PersonEntity(name = name, phoneNumber = phone))
        }
    }

    fun addLedgerTransaction(tx: LedgerTransactionEntity) {
        viewModelScope.launch {
            repository.insertLedgerTransaction(tx)
        }
    }

    /** Sprint 5 — edit flow for debts: updates an existing ledger transaction in place. */
    fun updateLedgerTransaction(tx: LedgerTransactionEntity) {
        viewModelScope.launch {
            repository.updateLedgerTransaction(tx)
        }
    }

    fun deleteLedgerTransaction(tx: LedgerTransactionEntity) {
        viewModelScope.launch {
            repository.deleteLedgerTransaction(tx)
        }
    }

    /**
     * Sprint 4 — Smart Debt.
     *
     * Creates a debt by COMPOSING the existing Ledger implementation:
     *  - person       → existing [PersonEntity] (or a new one via the
     *                   existing repository.insertPerson).
     *  - the debt     → the existing [LedgerTransactionEntity]
     *                   (GAVE_THEM = lent / THEY_GAVE_ME = borrowed).
     *  - due date     → the existing reminder pipeline
     *                   ([insertAndScheduleReminder]) linked through the
     *                   entity's existing linkedReminderId column.
     * No new entities, no duplicated debt logic, no new scheduler.
     */
    fun addDebt(
        existingPersonId: Long?,
        personName: String,
        amount: Double,
        isLent: Boolean,
        dueDate: Long?,
        note: String
    ) {
        viewModelScope.launch {
            val personId = existingPersonId
                ?: repository.insertPerson(PersonEntity(name = personName))

            val linkedReminderId = if (dueDate != null && dueDate > System.currentTimeMillis()) {
                val isArabic = language.value == "ar"
                insertAndScheduleReminder(
                    ReminderEntity(
                        title = if (isArabic) "استحقاق دين: $personName" else "Debt due: $personName",
                        note = note,
                        dueDate = dueDate,
                        category = ReminderCategory.MONEY.name
                    )
                )
            } else null

            repository.insertLedgerTransaction(
                LedgerTransactionEntity(
                    personId = personId,
                    type = if (isLent) {
                        LedgerTransactionType.GAVE_THEM.name
                    } else {
                        LedgerTransactionType.THEY_GAVE_ME.name
                    },
                    amount = amount,
                    date = System.currentTimeMillis(),
                    note = note,
                    linkedReminderId = linkedReminderId
                )
            )
        }
    }

    // Gam3iya Actions
    fun getMembersForGam3iya(id: Long): Flow<List<Gam3iyaMemberEntity>> = repository.getMembersForGam3iya(id)

    fun createGam3iya(
        title: String,
        totalAmount: Double,
        monthlyInstallment: Double,
        memberNamesWithTurns: List<Pair<String, Int>>,
        startDate: Long
    ) {
        viewModelScope.launch {
            val gam3iya = Gam3iyaEntity(
                title = title,
                totalAmount = totalAmount,
                monthlyInstallment = monthlyInstallment,
                membersCount = memberNamesWithTurns.size,
                startDate = startDate
            )
            repository.createGam3iyaWithMembers(gam3iya, memberNamesWithTurns)
        }
    }

    // Alarm Actions
    fun addAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val id = repository.insertAlarm(alarm)
            val fullAlarm = alarm.copy(id = id)
            AlarmManagerScheduler.scheduleExactAlarm(getApplication(), fullAlarm)
        }
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.updateAlarm(updated)
            if (updated.isEnabled) {
                AlarmManagerScheduler.scheduleExactAlarm(getApplication(), updated)
            } else {
                AlarmManagerScheduler.cancelAlarm(getApplication(), updated.id)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
            AlarmManagerScheduler.cancelAlarm(getApplication(), alarm.id)
        }
    }

    // Work Note Actions
    fun addWorkNote(title: String, content: String, reminderTime: Long? = null) {
        viewModelScope.launch {
            repository.insertWorkNote(
                WorkNoteEntity(title = title, content = content, reminderTime = reminderTime)
            )
        }
    }

    fun toggleWorkNoteDone(note: WorkNoteEntity) {
        viewModelScope.launch {
            repository.updateWorkNote(note.copy(isDone = !note.isDone))
        }
    }

    fun deleteWorkNote(note: WorkNoteEntity) {
        viewModelScope.launch {
            repository.deleteWorkNote(note)
        }
    }

    fun incrementWater() {
        _waterCount.value++
    }

    // AI Chat
    fun sendAiMessage(userText: String) {
        if (userText.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            val (reply, newHistory) = geminiRepository.sendMessage(
                history = _chatMessages.value,
                userMessage = userText,
                customApiKey = geminiApiKey.value,
                onAlarmCreated = { alarm ->
                    AlarmManagerScheduler.scheduleExactAlarm(getApplication(), alarm)
                }
            )
            _chatMessages.value = newHistory
            _isAiLoading.value = false
        }
    }

    // User Preferences Actions
    fun setLanguage(lang: String) {
        viewModelScope.launch { preferencesRepository.setLanguage(lang) }
    }

    fun setDarkMode(isDark: Boolean) {
        viewModelScope.launch { preferencesRepository.setDarkMode(isDark) }
    }

    fun setUserAuth(name: String, email: String, isLoggedIn: Boolean) {
        viewModelScope.launch { preferencesRepository.setUserAuth(name, email, isLoggedIn) }
    }

    fun setGeminiApiKey(key: String) {
        viewModelScope.launch { preferencesRepository.setGeminiApiKey(key) }
    }

    fun setAlarmRingtoneUri(uri: String) {
        viewModelScope.launch { preferencesRepository.setAlarmRingtoneUri(uri) }
    }

    private val backupRepository = com.notification.app.data.repository.BackupRepository(db)

    private val _backupState = MutableStateFlow<String?>(null)
    val backupState: StateFlow<String?> = _backupState.asStateFlow()

    fun triggerBackupSync() {
        viewModelScope.launch {
            when (val result = backupRepository.backupNow()) {
                is com.notification.app.data.repository.BackupRepository.BackupResult.Success -> {
                    preferencesRepository.updateLastSyncTime(System.currentTimeMillis())
                    _backupState.value = "success"
                }
                is com.notification.app.data.repository.BackupRepository.BackupResult.Failure -> {
                    _backupState.value = "error: ${result.message}"
                }
            }
        }
    }

    /** Called right after a successful sign-in to pull any existing cloud backup down. */
    fun restoreFromBackup() {
        viewModelScope.launch {
            backupRepository.restoreLatest()
        }
    }
}
