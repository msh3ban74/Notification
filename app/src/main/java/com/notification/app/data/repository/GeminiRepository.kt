package com.notification.app.data.repository

import com.notification.app.BuildConfig
import com.notification.app.data.local.entities.AlarmEntity
import com.notification.app.data.local.entities.LedgerTransactionEntity
import com.notification.app.data.local.entities.PersonEntity
import com.notification.app.data.local.entities.ReminderEntity
import com.notification.app.data.remote.*
import com.notification.app.domain.calculator.Gam3iyaCalculator
import com.notification.app.domain.calculator.HabitCalculator
import com.notification.app.domain.calculator.LedgerCalculator
import com.notification.app.domain.model.AiSuggestion
import com.notification.app.domain.model.AiSuggestionAction
import com.notification.app.domain.model.LedgerTransactionType
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class GeminiRepository(
    private val notificationRepository: NotificationRepository
) {

    private val toolsDefinition = listOf(
        GeminiTool(
            functionDeclarations = listOf(
                GeminiFunctionDeclaration(
                    name = "getReminders",
                    description = "Get a list of all upcoming reminders and due dates in the app.",
                    parameters = mapOf("type" to "OBJECT", "properties" to emptyMap<String, Any>())
                ),
                GeminiFunctionDeclaration(
                    name = "addReminder",
                    description = "Create a new reminder for the user.",
                    parameters = mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "title" to mapOf("type" to "STRING", "description" to "Reminder title"),
                            "note" to mapOf("type" to "STRING", "description" to "Optional note"),
                            "dueDateMillis" to mapOf("type" to "NUMBER", "description" to "Epoch timestamp in milliseconds for due date/time"),
                            "category" to mapOf("type" to "STRING", "description" to "Category: MONEY, APPOINTMENT, BIRTHDAY, BILL, TUTORING, or CUSTOM")
                        ),
                        "required" to listOf("title", "dueDateMillis")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "getDebtsAndLedger",
                    description = "Get summary of all debts, loans, and net balances per person.",
                    parameters = mapOf("type" to "OBJECT", "properties" to emptyMap<String, Any>())
                ),
                GeminiFunctionDeclaration(
                    name = "addLedgerTransaction",
                    description = "Add a debt or loan transaction for a person.",
                    parameters = mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "personName" to mapOf("type" to "STRING", "description" to "Name of the person"),
                            "transactionType" to mapOf("type" to "STRING", "description" to "Type: GAVE_THEM (I lent money), THEY_GAVE_BACK (they repaid), THEY_GAVE_ME (I borrowed money), I_GAVE_BACK (I repaid)"),
                            "amount" to mapOf("type" to "NUMBER", "description" to "Transaction amount in EGP"),
                            "note" to mapOf("type" to "STRING", "description" to "Optional note")
                        ),
                        "required" to listOf("personName", "transactionType", "amount")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "getGam3iyaInfo",
                    description = "Get status of savings circles (gam3iya) and member payout dates.",
                    parameters = mapOf("type" to "OBJECT", "properties" to emptyMap<String, Any>())
                ),
                // Phase E — the assistant reads ALL modules.
                GeminiFunctionDeclaration(
                    name = "getFinancialItems",
                    description = "Get all tracked bills, installments and subscriptions with amounts, due dates and paid status.",
                    parameters = mapOf("type" to "OBJECT", "properties" to emptyMap<String, Any>())
                ),
                GeminiFunctionDeclaration(
                    name = "getHabits",
                    description = "Get the user's habits with current streaks and whether each is done today.",
                    parameters = mapOf("type" to "OBJECT", "properties" to emptyMap<String, Any>())
                ),
                GeminiFunctionDeclaration(
                    name = "setSmartAlarm",
                    description = "Set an alarm for a specific date and time.",
                    parameters = mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "title" to mapOf("type" to "STRING", "description" to "Alarm label/title"),
                            "timestampMillis" to mapOf("type" to "NUMBER", "description" to "Target epoch timestamp in milliseconds")
                        ),
                        "required" to listOf("title", "timestampMillis")
                    )
                )
            )
        )
    )

    suspend fun sendMessage(
        history: List<GeminiContent>,
        userMessage: String,
        customApiKey: String? = null,
        onAlarmCreated: suspend (AlarmEntity) -> Unit = {}
    ): Pair<String, List<GeminiContent>> {
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else BuildConfig.GEMINI_API_KEY
        val updatedHistory = history.toMutableList()

        // Append user message
        updatedHistory.add(
            GeminiContent(role = "user", parts = listOf(GeminiPart(text = userMessage)))
        )

        val systemInstruction = GeminiContent(
            parts = listOf(
                GeminiPart(
                    text = "You are Rafeeq Smart Assistant (مساعد رفيق الذكي), a smart, executive, bilingual (Arabic/English) assistant for managing reminders, per-person debts/ledgers, gam3iyas, bills/installments/subscriptions, habits, prayer times, work notes, and setting alarms. Never mention underlying AI model providers or internal names like Gemini in responses. Use function calls whenever the user asks about or wants to manage reminders, ledger entries, gam3iya details, money items, habits, or alarms. Never invent data — read it with the tools. Current time: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}."
                )
            )
        )

        try {
            val request = GeminiRequest(
                contents = updatedHistory,
                tools = toolsDefinition,
                systemInstruction = systemInstruction
            )

            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            val candidateContent = response.candidates?.firstOrNull()?.content

            if (candidateContent != null) {
                updatedHistory.add(candidateContent)
                val part = candidateContent.parts.firstOrNull()

                if (part?.functionCall != null) {
                    val fnCall = part.functionCall
                    val toolResult = executeTool(fnCall, onAlarmCreated)

                    // Provide function response back to model
                    val toolResponseContent = GeminiContent(
                        role = "user",
                        parts = listOf(
                            GeminiPart(
                                functionResponse = GeminiFunctionResponse(
                                    name = fnCall.name,
                                    response = mapOf("result" to toolResult)
                                )
                            )
                        )
                    )
                    updatedHistory.add(toolResponseContent)

                    // Second turn after tool call
                    val followUpRequest = GeminiRequest(
                        contents = updatedHistory,
                        tools = toolsDefinition,
                        systemInstruction = systemInstruction
                    )
                    val followUpResponse = RetrofitClient.geminiService.generateContent(apiKey, followUpRequest)
                    val finalText = followUpResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "Action completed: $toolResult"

                    updatedHistory.add(
                        GeminiContent(role = "model", parts = listOf(GeminiPart(text = finalText)))
                    )
                    return Pair(finalText, updatedHistory)
                } else if (part?.text != null) {
                    return Pair(part.text, updatedHistory)
                }
            }

            // No parsable candidate — surface it as a VISIBLE model bubble
            // (returning text without appending it left the chat empty).
            val fallback = "لم أستطع فهم الرد هذه المرة — جرّب صياغة أخرى 🙏\nI couldn't process that — please try rephrasing."
            updatedHistory.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = fallback))))
            return Pair(fallback, updatedHistory)
        } catch (e: Exception) {
            val errorText = "عذراً، تعذّر الوصول للمساعد الآن — تأكد من الاتصال وحاول مجددًا 🙏\n" +
                "Sorry, I couldn't reach the assistant — check your connection and try again."
            updatedHistory.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = errorText))))
            return Pair(errorText, updatedHistory)
        }
    }

    /**
     * Dashboard "Rafeeq Suggestions" — generated by the SAME Gemini
     * pipeline (same Retrofit service, same API key resolution, same
     * repository) from the user's REAL data. Returns an empty list when
     * there is no data worth advising on or when the model/network
     * fails, so the UI can fall back to its local rule-based insights.
     *
     * The model must answer with a strict JSON array of
     * {"text": ..., "action": OPEN_TASKS|OPEN_NOTIFICATIONS|OPEN_LEDGER|ASK_RAFEEQ}
     * — actions map only to EXISTING flows.
     */
    suspend fun generateDashboardSuggestions(
        isArabic: Boolean,
        customApiKey: String? = null
    ): List<AiSuggestion> {
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else BuildConfig.GEMINI_API_KEY
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
        val now = System.currentTimeMillis()

        // Real data snapshot from the EXISTING repository flows.
        val reminders = notificationRepository.allReminders.first()
        val persons = notificationRepository.allPersons.first()
        val transactions = notificationRepository.allTransactions.first()
        val alarms = notificationRepository.allAlarms.first()
        // Phase E — the AI reads ALL modules.
        val financialItems = notificationRepository.allFinancialItems.first()
        val habits = notificationRepository.allHabits.first()
        val habitLogs = notificationRepository.allHabitLogs.first()

        if (reminders.isEmpty() && persons.isEmpty() && alarms.isEmpty() &&
            financialItems.isEmpty() && habits.isEmpty()
        ) return emptyList()

        val pending = reminders.filter { !it.isCompleted && !it.isArchived }
        val contextText = buildString {
            appendLine("CURRENT TIME: ${dateFormat.format(Date(now))}")
            appendLine("PENDING ITEMS (title | category | due | overdue?):")
            pending.sortedBy { it.dueDate }.take(15).forEach {
                appendLine("- ${it.title} | ${it.category} | ${dateFormat.format(Date(it.dueDate))} | ${if (it.dueDate < now) "OVERDUE" else "upcoming"}")
            }
            appendLine("DEBT BALANCES (person | status | net EGP):")
            persons.take(10).forEach { person ->
                val txs = transactions.filter { it.personId == person.id }
                val summary = LedgerCalculator.calculateNetBalance(txs)
                appendLine("- ${person.name} | ${summary.status} | ${summary.netAmount}")
            }
            appendLine("ENABLED FUTURE ALARMS: ${alarms.count { it.isEnabled && it.timeInMillis >= now }}")
            val unpaid = financialItems.filter { !it.isPaid }.sortedBy { it.dueDate }
            if (unpaid.isNotEmpty()) {
                appendLine("UNPAID MONEY ITEMS (title | type | EGP | due):")
                unpaid.take(10).forEach {
                    val amount = if (it.monthlyAmount > 0) it.monthlyAmount else it.amount
                    appendLine("- ${it.title} | ${it.type} | $amount | ${dateFormat.format(Date(it.dueDate))}")
                }
            }
            if (habits.isNotEmpty()) {
                val today = HabitCalculator.dayStartOf(now)
                val daysByHabit = habitLogs.groupBy({ it.habitId }, { it.dayStart }).mapValues { it.value.toSet() }
                appendLine("HABITS (name | streak days | done today?):")
                habits.take(10).forEach { habit ->
                    val days = daysByHabit[habit.id] ?: emptySet()
                    appendLine("- ${habit.title} | ${HabitCalculator.currentStreak(days, today)} | ${if (today in days) "yes" else "NO"}")
                }
            }
        }

        val language = if (isArabic) "Arabic" else "English"
        val prompt = """
            You are Rafeeq, the user's premium personal life assistant.
            Based ONLY on the user data below, write the 3 most useful,
            concise, actionable suggestions for the user's day in $language.
            Never invent data that is not present. Vary the topics.

            For each suggestion choose exactly one action from:
            OPEN_TASKS, OPEN_NOTIFICATIONS, OPEN_LEDGER, ASK_RAFEEQ.

            Respond with ONLY a raw JSON array, no markdown, no code fences:
            [{"text":"...","action":"OPEN_TASKS"}]

            USER DATA:
            $contextText
        """.trimIndent()

        return try {
            val request = GeminiRequest(
                contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt))))
            )
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            val raw = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return emptyList()

            // Tolerate accidental code fences / prose around the array.
            val jsonText = raw.substring(
                raw.indexOf('[').takeIf { it >= 0 } ?: return emptyList(),
                raw.lastIndexOf(']').takeIf { it >= 0 }?.plus(1) ?: return emptyList()
            )
            val array = org.json.JSONArray(jsonText)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val text = obj.optString("text").trim()
                if (text.isBlank()) null
                else AiSuggestion(text = text, action = AiSuggestionAction.fromString(obj.optString("action")))
            }.take(3)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun executeTool(
        call: GeminiFunctionCall,
        onAlarmCreated: suspend (AlarmEntity) -> Unit
    ): Any {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return when (call.name) {
            "getReminders" -> {
                val reminders = notificationRepository.allReminders.first()
                if (reminders.isEmpty()) "No reminders found."
                else reminders.map { "${it.title} (Due: ${dateFormat.format(Date(it.dueDate))})" }
            }
            "addReminder" -> {
                val title = call.args["title"]?.toString() ?: "New Reminder"
                val note = call.args["note"]?.toString() ?: ""
                val due = (call.args["dueDateMillis"] as? Number)?.toLong() ?: (System.currentTimeMillis() + 3600000)
                val cat = call.args["category"]?.toString() ?: "CUSTOM"

                val id = notificationRepository.insertReminder(
                    ReminderEntity(title = title, note = note, dueDate = due, category = cat)
                )
                "Reminder '$title' added successfully (ID: $id)."
            }
            "getDebtsAndLedger" -> {
                val persons = notificationRepository.allPersons.first()
                if (persons.isEmpty()) "No ledger records found."
                else {
                    persons.map { person ->
                        val txs = notificationRepository.getTransactionsForPerson(person.id).first()
                        val summary = LedgerCalculator.calculateNetBalance(txs)
                        "${person.name}: Status=${summary.status}, Net=${summary.netAmount} EGP"
                    }
                }
            }
            "addLedgerTransaction" -> {
                val name = call.args["personName"]?.toString() ?: "Contact"
                val type = call.args["transactionType"]?.toString() ?: "GAVE_THEM"
                val amount = (call.args["amount"] as? Number)?.toDouble() ?: 0.0
                val note = call.args["note"]?.toString() ?: ""

                val persons = notificationRepository.allPersons.first()
                var person = persons.firstOrNull { it.name.equals(name, ignoreCase = true) }
                if (person == null) {
                    val personId = notificationRepository.insertPerson(PersonEntity(name = name))
                    person = PersonEntity(id = personId, name = name)
                }

                notificationRepository.insertLedgerTransaction(
                    LedgerTransactionEntity(
                        personId = person.id,
                        type = type,
                        amount = amount,
                        date = System.currentTimeMillis(),
                        note = note
                    )
                )
                "Ledger transaction recorded for ${person.name}: $amount EGP ($type)."
            }
            "getGam3iyaInfo" -> {
                val gam3iyas = notificationRepository.allGam3iyas.first()
                if (gam3iyas.isEmpty()) "No active gam3iyas."
                else {
                    gam3iyas.map { g ->
                        val members = notificationRepository.getMembersForGam3iya(g.id).first()
                        val summary = Gam3iyaCalculator.calculateSummary(g, members)
                        val memberTurns = members.map { "${it.memberName} (Month ${it.turnMonth}: ${dateFormat.format(Date(it.payoutDate))})" }
                        "Gam3iya '${g.title}': Total ${g.totalAmount} EGP, Duration ${summary.durationMonths} months. Turns: ${memberTurns.joinToString(", ")}"
                    }
                }
            }
            // Phase E — read-only views over the financial + habit modules.
            "getFinancialItems" -> {
                val items = notificationRepository.allFinancialItems.first()
                if (items.isEmpty()) "No bills, installments or subscriptions tracked."
                else {
                    items.sortedBy { it.dueDate }.map {
                        val amount = if (it.monthlyAmount > 0) it.monthlyAmount else it.amount
                        "${it.title} [${it.type}]: $amount EGP, due ${dateFormat.format(Date(it.dueDate))}, ${if (it.isPaid) "PAID" else "UNPAID"}" +
                            (if (it.remaining > 0) ", remaining ${it.remaining} EGP" else "")
                    }
                }
            }
            "getHabits" -> {
                val habits = notificationRepository.allHabits.first()
                if (habits.isEmpty()) "No habits tracked yet."
                else {
                    val logs = notificationRepository.allHabitLogs.first()
                    val today = HabitCalculator.dayStartOf()
                    val daysByHabit = logs.groupBy({ it.habitId }, { it.dayStart }).mapValues { it.value.toSet() }
                    habits.map { habit ->
                        val days = daysByHabit[habit.id] ?: emptySet()
                        "${habit.emoji} ${habit.title}: streak ${HabitCalculator.currentStreak(days, today)} days, " +
                            (if (today in days) "done today" else "NOT done today")
                    }
                }
            }
            "setSmartAlarm" -> {
                val title = call.args["title"]?.toString() ?: "Smart Alarm"
                val targetMillis = (call.args["timestampMillis"] as? Number)?.toLong() ?: (System.currentTimeMillis() + 600000)

                val alarm = AlarmEntity(
                    title = title,
                    timeInMillis = targetMillis,
                    isEnabled = true,
                    createdViaAi = true
                )
                val alarmId = notificationRepository.insertAlarm(alarm)
                val fullAlarm = alarm.copy(id = alarmId)
                onAlarmCreated(fullAlarm)
                "Alarm '$title' scheduled for ${dateFormat.format(Date(targetMillis))}."
            }
            else -> "Unknown function call: ${call.name}"
        }
    }
}
