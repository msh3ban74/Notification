package com.notification.app.domain.model

/**
 * A single actionable suggestion for the Dashboard's "Rafeeq Suggestions"
 * card, produced by the EXISTING Gemini pipeline from the user's real
 * data. The action is a closed enum — each value maps to an EXISTING
 * flow in the app (navigation or the assistant), never to new logic.
 */
data class AiSuggestion(
    val text: String,
    val action: AiSuggestionAction
)

enum class AiSuggestionAction {
    /** Open the Tasks tab (existing navigation). */
    OPEN_TASKS,

    /** Open the Notifications tab (existing navigation). */
    OPEN_NOTIFICATIONS,

    /** Open the debts ledger (existing navigation). */
    OPEN_LEDGER,

    /** Hand the suggestion to the existing AI assistant conversation. */
    ASK_RAFEEQ;

    companion object {
        fun fromString(value: String?): AiSuggestionAction =
            entries.find { it.name.equals(value?.trim(), ignoreCase = true) } ?: ASK_RAFEEQ
    }
}
