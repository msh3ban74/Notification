package com.notification.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notification.app.data.local.AppDatabase
import com.notification.app.data.local.entities.*
import com.notification.app.data.preferences.UserPreferencesRepository
import android.util.Log
import com.notification.app.BuildConfig
import com.notification.app.data.remote.GeminiContent
import com.notification.app.data.remote.GeminiPart
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
import kotlinx.coroutines.withTimeoutOrNull
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

    val lastSyncTime: StateFlow<Long> = preferencesRepository.lastSyncTimeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val alarmRingtoneUri: StateFlow<String> = preferencesRepository.alarmRingtoneUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Database Flows.
    // Sprint 6 — performance: core dashboard flows use SharingStarted.Lazily
    // so Room keeps them HOT for the ViewModel's whole life. Switching tabs
    // never tears the subscription down, so returning to the Dashboard (or
    // any list screen) renders instantly from the cached StateFlow value —
    // Room pushes fresh values in the background when data changes.
    val allReminders: StateFlow<List<ReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pendingReminders: StateFlow<List<ReminderEntity>> = repository.pendingReminders
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allPersons: StateFlow<List<PersonEntity>> = repository.allPersons
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allTransactions: StateFlow<List<LedgerTransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allGam3iyas: StateFlow<List<Gam3iyaEntity>> = repository.allGam3iyas
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Sprint 6 — Executive Dashboard: upcoming gam3iya payouts widget.
    val allGam3iyaMembers: StateFlow<List<Gam3iyaMemberEntity>> = repository.allGam3iyaMembers
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allAlarms: StateFlow<List<AlarmEntity>> = repository.allAlarms
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allWorkNotes: StateFlow<List<WorkNoteEntity>> = repository.allWorkNotes
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Phase B — financial items (bills / installments / subscriptions).
    val allFinancialItems: StateFlow<List<FinancialItemEntity>> = repository.allFinancialItems
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addFinancialItem(item: FinancialItemEntity) {
        viewModelScope.launch { repository.insertFinancialItem(item) }
    }

    fun updateFinancialItem(item: FinancialItemEntity) {
        viewModelScope.launch { repository.updateFinancialItem(item) }
    }

    fun deleteFinancialItem(item: FinancialItemEntity) {
        viewModelScope.launch { repository.deleteFinancialItem(item) }
    }

    // Phase C — habit engine. The log flow feeds HabitCalculator
    // (streaks / calendar / percentages) in the UI layer.
    val allHabits: StateFlow<List<HabitEntity>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allHabitLogs: StateFlow<List<HabitLogEntity>> = repository.allHabitLogs
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addHabit(habit: HabitEntity) {
        viewModelScope.launch { repository.insertHabit(habit) }
    }

    fun updateHabit(habit: HabitEntity) {
        viewModelScope.launch { repository.updateHabit(habit) }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch { repository.deleteHabit(habit) }
    }

    fun setHabitDone(habitId: Long, dayStart: Long, done: Boolean) {
        viewModelScope.launch { repository.setHabitDone(habitId, dayStart, done) }
    }

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

    // Sprint 6 — TTL cache for AI suggestions: within the TTL the cached
    // list is served instantly with no network call; pull-to-refresh
    // passes force=true to bypass. The UI is never blocked either way.
    private var aiSuggestionsFetchedAt = 0L

    /**
     * Refreshes the dashboard AI suggestions. Reuses the existing Gemini
     * repository and API-key resolution; on failure the previous list is
     * kept (or stays empty, letting the dashboard fall back to its local
     * rule-based insights).
     */
    fun refreshAiSuggestions(isArabic: Boolean, force: Boolean = false) {
        if (_aiSuggestionsLoading.value) return
        val fresh = System.currentTimeMillis() - aiSuggestionsFetchedAt < AI_SUGGESTIONS_TTL_MS
        if (!force && _aiSuggestions.value.isNotEmpty() && fresh) return
        viewModelScope.launch {
            _aiSuggestionsLoading.value = true
            val result = geminiRepository.generateDashboardSuggestions(
                isArabic = isArabic
            )
            if (result.isNotEmpty()) {
                _aiSuggestions.value = result
                aiSuggestionsFetchedAt = System.currentTimeMillis()
            }
            _aiSuggestionsLoading.value = false
        }
    }

    /**
     * Stability sprint — full logout: Firebase sign-out happens at the
     * call site; here we clear the persisted session and every piece of
     * cached user-scoped state so the app is truly signed out (and stays
     * signed out after restart via the persisted isLoggedIn=false).
     */
    fun onLogout() {
        viewModelScope.launch {
            preferencesRepository.setUserAuth("Guest User", "", false)
        }
        _chatMessages.value = emptyList()
        _aiSuggestions.value = emptyList()
        aiSuggestionsFetchedAt = 0L
        _isAiLoading.value = false
    }

    companion object {
        /** How long cached AI suggestions stay fresh before a silent re-fetch. */
        const val AI_SUGGESTIONS_TTL_MS: Long = 15 * 60 * 1000L

        /** Hard ceiling for a single AI generation before a friendly cancel. */
        const val AI_GENERATION_TIMEOUT_MS: Long = 15_000L
    }

    // Water counter state
    private val _waterCount = MutableStateFlow(0)
    val waterCount: StateFlow<Int> = _waterCount.asStateFlow()

    init {
        // Stability sprint — runtime verification of the key pipeline
        // (GitHub Secret -> .env -> BuildConfig). Logs availability ONLY.
        Log.i("Rafeeq", "API Key Available = " + BuildConfig.GEMINI_API_KEY.isNotBlank())
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

    // Phase D — CRUD extras (pin / archive / duplicate).
    fun toggleReminderPinned(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isPinned = !reminder.isPinned))
        }
    }

    fun setReminderArchived(reminder: ReminderEntity, archived: Boolean) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isArchived = archived))
        }
    }

    /**
     * Duplicate rides the SAME insert-and-schedule pipeline as a new
     * reminder, so the copy gets its own alarms. The copy starts fresh:
     * not completed, not pinned, id 0 (auto-generated).
     */
    fun duplicateReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            insertAndScheduleReminder(
                reminder.copy(
                    id = 0,
                    isCompleted = false,
                    isPinned = false,
                    isArchived = false,
                    createdAt = System.currentTimeMillis()
                )
            )
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
    /**
     * Stability sprint — reliable AI pipeline:
     *  - the user's message appears in the conversation IMMEDIATELY
     *    (optimistic append) and the typing indicator follows instantly;
     *  - single-flight: while a generation is active, new sends are
     *    ignored, so duplicate requests are impossible;
     *  - a hard 15s timeout cancels a hung generation and posts a
     *    friendly retry message — the conversation can never freeze;
     *  - repository-level failures (network / API / parsing) already
     *    return readable error replies, so nothing fails silently;
     *  - the API key comes from BuildConfig only (no Settings key).
     */
    fun sendAiMessage(userText: String) {
        if (userText.isBlank()) return
        if (_isAiLoading.value) return
        val previousHistory = _chatMessages.value

        // Optimistic UI: show the user's bubble right away.
        _chatMessages.value = previousHistory +
            GeminiContent(role = "user", parts = listOf(GeminiPart(text = userText)))
        _isAiLoading.value = true

        viewModelScope.launch {
            val result = withTimeoutOrNull(AI_GENERATION_TIMEOUT_MS) {
                geminiRepository.sendMessage(
                    history = previousHistory,
                    userMessage = userText,
                    onAlarmCreated = { alarm ->
                        AlarmManagerScheduler.scheduleExactAlarm(getApplication(), alarm)
                    }
                )
            }
            _chatMessages.value = if (result != null) {
                result.second
            } else {
                _chatMessages.value + GeminiContent(
                    role = "model",
                    parts = listOf(
                        GeminiPart(
                            text = "استغرق الرد وقتًا أطول من المعتاد فأوقفته — جرّب مرة أخرى الآن 🙏\n" +
                                "That took longer than usual, so I stopped it — please try again."
                        )
                    )
                )
            }
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

    fun setAlarmRingtoneUri(uri: String) {
        viewModelScope.launch { preferencesRepository.setAlarmRingtoneUri(uri) }
    }

    private val backupRepository = com.notification.app.data.repository.BackupRepository(db)

    private val _backupState = MutableStateFlow<String?>(null)
    val backupState: StateFlow<String?> = _backupState.asStateFlow()

    fun triggerBackupSync() {
        if (_backupState.value == "syncing") return
        _backupState.value = "syncing"
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
