package com.notification.app.domain.model

enum class LedgerTransactionType(
    val displayNameEn: String,
    val displayNameAr: String,
    val isGivingToThem: Boolean
) {
    GAVE_THEM("I gave them (Lent)", "لهم (أقرضتهم)", true),
    THEY_GAVE_BACK("They gave back (Repaid)", "استلمت مني (سددوا لي)", false),
    THEY_GAVE_ME("They gave me (Borrowed)", "لي (اقترضت منهم)", false),
    I_GAVE_BACK("I gave back (Repaid)", "سددت لهم (سددت لهم)", true);

    companion object {
        fun fromString(value: String): LedgerTransactionType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: GAVE_THEM
        }
    }
}
