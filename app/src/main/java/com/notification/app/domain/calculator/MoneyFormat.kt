package com.notification.app.domain.calculator

import java.util.Locale

/**
 * One place to turn a money [Double] into text. Amounts are stored as Double,
 * so raw interpolation could surface floating-point noise like
 * "1500.0000000002". This rounds to the piastre (2 decimals) and drops any
 * trailing ".00", so whole pounds read as "1500" and "12.50" stays "12.5" →
 * clean, never scary. Use this everywhere an amount is shown to the user.
 */
object MoneyFormat {
    fun format(amount: Double): String {
        val rounded = Math.round(amount * 100.0) / 100.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
        }
    }
}
