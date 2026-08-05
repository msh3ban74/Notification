package com.notification.app.domain.model

/**
 * The three money Smart Items backed by FinancialItemEntity.
 */
enum class FinancialType(
    val displayEn: String,
    val displayAr: String
) {
    BILL("Bill", "فاتورة"),
    INSTALLMENT("Installment", "قسط"),
    SUBSCRIPTION("Subscription", "اشتراك");

    companion object {
        fun fromString(value: String?): FinancialType =
            entries.find { it.name.equals(value?.trim(), ignoreCase = true) } ?: BILL

        fun fromItemId(itemId: String?): FinancialType = when (itemId) {
            "installment" -> INSTALLMENT
            "subscription" -> SUBSCRIPTION
            else -> BILL
        }
    }
}
