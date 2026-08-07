package com.notification.app.domain.calculator

import android.content.Context
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.notification.app.data.local.entities.LedgerTransactionEntity
import com.notification.app.data.local.entities.PersonEntity
import com.notification.app.domain.model.LedgerTransactionType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sprint 5 — export one person's ledger as CSV (saved via SAF) or as a
 * printable / PDF statement (system print dialog → print or "Save as PDF").
 * Free-text sharing stays in [StatementExporter]; this mirrors
 * [Gam3iyaExporter] so the two export flows behave identically.
 */
object DebtsExporter {

    private val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dfLong = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    // Keep a strong ref so the WebView isn't GC'd before the print spooler runs.
    private var printWebView: WebView? = null

    private fun typeLabel(type: String): String = when (LedgerTransactionType.fromString(type)) {
        LedgerTransactionType.GAVE_THEM -> "Lent"
        LedgerTransactionType.THEY_GAVE_BACK -> "They repaid"
        LedgerTransactionType.THEY_GAVE_ME -> "Borrowed"
        LedgerTransactionType.I_GAVE_BACK -> "I repaid"
    }

    /** +ve moves the balance toward "they owe me", -ve toward "I owe them". */
    private fun signedDelta(tx: LedgerTransactionEntity): Double =
        when (LedgerTransactionType.fromString(tx.type)) {
            LedgerTransactionType.GAVE_THEM -> tx.amount
            LedgerTransactionType.THEY_GAVE_BACK -> -tx.amount
            LedgerTransactionType.THEY_GAVE_ME -> -tx.amount
            LedgerTransactionType.I_GAVE_BACK -> tx.amount
        }

    fun buildCsv(person: PersonEntity, transactions: List<LedgerTransactionEntity>): String {
        val summary = LedgerCalculator.calculateNetBalance(transactions)
        val sb = StringBuilder()
        sb.append("Contact,${esc(person.name)}\n")
        if (person.phoneNumber.isNotBlank()) sb.append("Phone,${esc(person.phoneNumber)}\n")
        if (person.company.isNotBlank()) sb.append("Company,${esc(person.company)}\n")
        sb.append("Status,${summary.status}\n")
        sb.append("Net balance,${summary.netAmount}\n")
        sb.append("Total lent,${summary.totalLent}\n")
        sb.append("Total repaid to me,${summary.totalRepaidToMe}\n")
        sb.append("Total borrowed,${summary.totalBorrowed}\n")
        sb.append("Total I repaid,${summary.totalIRepaid}\n\n")

        sb.append("Date,Type,Amount,Currency,Reason,Note,Running balance\n")
        var running = 0.0
        transactions.sortedBy { it.date }.forEach { tx ->
            running += signedDelta(tx)
            sb.append(
                "${if (tx.date > 0) df.format(Date(tx.date)) else ""}," +
                    "${typeLabel(tx.type)},${tx.amount},${esc(tx.currency)}," +
                    "${esc(tx.reason)},${esc(tx.note)},$running\n"
            )
        }
        return sb.toString()
    }

    /** Writes CSV text to a user-chosen SAF uri. Returns true on success. */
    fun writeCsvToUri(context: Context, uri: Uri, csv: String): Boolean = try {
        context.contentResolver.openOutputStream(uri, "w")?.use { it.write(csv.toByteArray(Charsets.UTF_8)); it.flush() }
        true
    } catch (e: Exception) { false }

    fun suggestedCsvName(person: PersonEntity): String =
        "Statement_${person.name.replace(Regex("[^A-Za-z0-9]"), "_").take(24)}.csv"

    private fun buildHtml(person: PersonEntity, transactions: List<LedgerTransactionEntity>, isArabic: Boolean): String {
        val dir = if (isArabic) "rtl" else "ltr"
        val align = if (isArabic) "right" else "left"
        val summary = LedgerCalculator.calculateNetBalance(transactions)
        val statusText = when (summary.status) {
            LedgerStatus.THEY_OWE_ME -> if (isArabic) "لك (مستحق)" else "They owe you"
            LedgerStatus.I_OWE_THEM -> if (isArabic) "عليك" else "You owe them"
            LedgerStatus.SETTLED -> if (isArabic) "مسوّاة" else "Settled"
        }
        var running = 0.0
        val rows = transactions.sortedBy { it.date }.joinToString("") { tx ->
            running += signedDelta(tx)
            val date = if (tx.date > 0) dfLong.format(Date(tx.date)) else "-"
            val t = LedgerTransactionType.fromString(tx.type)
            val typeText = if (isArabic) t.displayNameAr else t.displayNameEn
            "<tr><td>$date</td><td>$typeText</td><td>${trim(tx.amount)} ${esc(tx.currency)}</td>" +
                "<td>${esc(tx.reason)}</td><td>${trim(running)}</td></tr>"
        }
        val header = if (isArabic)
            "<tr><th>التاريخ</th><th>النوع</th><th>المبلغ</th><th>السبب</th><th>الرصيد</th></tr>"
        else
            "<tr><th>Date</th><th>Type</th><th>Amount</th><th>Reason</th><th>Balance</th></tr>"
        return """
            <html dir="$dir"><head><meta charset="utf-8">
            <style>
              body{font-family:sans-serif;padding:24px;color:#111}
              h1{color:#8a6d1a;margin-bottom:4px}
              .sub{color:#555;margin:0 0 12px}
              table{width:100%;border-collapse:collapse;margin-top:16px}
              th,td{border:1px solid #ccc;padding:8px;text-align:$align}
              th{background:#f3ecd6}
            </style></head><body>
            <h1>${esc(person.name)}</h1>
            <p class="sub">${if (person.phoneNumber.isNotBlank()) esc(person.phoneNumber) + " • " else ""}${if (person.company.isNotBlank()) esc(person.company) else ""}</p>
            <p><b>$statusText:</b> ${trim(summary.netAmount)}</p>
            <table>$header$rows</table>
            </body></html>
        """.trimIndent()
    }

    /** Opens the Android print dialog (user can print or Save as PDF). */
    fun printStatement(context: Context, person: PersonEntity, transactions: List<LedgerTransactionEntity>, isArabic: Boolean) {
        val html = buildHtml(person, transactions, isArabic)
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
                val jobName = "Statement ${person.name}"
                val adapter = view.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, adapter, PrintAttributes.Builder().build())
                printWebView = null
            }
        }
        printWebView = webView
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun trim(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
    private fun esc(s: String): String = s.replace("\"", "\"\"").replace("\n", " ").replace(",", " ")
}
