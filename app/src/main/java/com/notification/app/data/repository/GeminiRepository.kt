package com.notification.app.data.repository

import com.notification.app.BuildConfig
import com.notification.app.data.local.entities.AlarmEntity
import com.notification.app.data.local.entities.LedgerTransactionEntity
import com.notification.app.data.local.entities.PersonEntity
import com.notification.app.data.local.entities.ReminderEntity
import com.notification.app.data.remote.*
import com.notification.app.domain.calculator.Gam3iyaCalculator
import com.notification.app.domain.calculator.LedgerCalculator
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
                    text = "You are Rafeeq Smart Assistant (مساعد رفيق الذكي), a smart, executive, bilingual (Arabic/English) assistant for managing reminders, per-person debts/ledgers, gam3iyas, prayer times, work notes, and setting alarms. Never mention underlying AI model providers or internal names like Gemini in responses. Use function calls whenever the user asks about or wants to manage reminders, ledger entries, gam3iya details, or alarms. Current time: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}."
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

            return Pair("I'm sorry, I couldn't process your request.", updatedHistory)
        } catch (e: Exception) {
            return Pair("عذراً، حدث خطأ أثناء التواصل مع المساعد الذكي: ${e.localizedMessage ?: "خطأ غير معروف"}", updatedHistory)
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
