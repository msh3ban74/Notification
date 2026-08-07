package com.notification.app.domain.calculator

import android.content.Context
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.notification.app.data.local.entities.FinancialItemEntity
import com.notification.app.data.local.entities.FinancialPaymentEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sprint 6 — export one installment as CSV (saved via SAF) or as a printable
 * / PDF payment plan + history (system print dialog → print or "Save as
 * PDF"). Derives everything from [FinancialCalculator.computePlan] so the
 * exported plan always matches what the detail screen shows.
 */
object InstallmentExporter {

    private val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dfLong = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private var printWebView: WebView? = null

    private fun paymentTypeLabel(type: String): String = when (type) {
        "FULL" -> "Full"
        "ADVANCE" -> "Advance"
        "EXTRA" -> "Extra"
        "RECURRING" -> "Monthly"
        else -> "Partial"
    }

    fun buildCsv(item: FinancialItemEntity, payments: List<FinancialPaymentEntity>): String {
        val plan = FinancialCalculator.computePlan(item, payments)
        val cur = item.currency
        val sb = StringBuilder()
        sb.append("Installment,${esc(item.title)}\n")
        if (item.brand.isNotBlank()) sb.append("Brand,${esc(item.brand)}\n")
        if (item.store.isNotBlank()) sb.append("Store,${esc(item.store)}\n")
        sb.append("Currency,$cur\n")
        sb.append("Total price,${item.totalPrice}\n")
        sb.append("Down payment,${item.downPayment}\n")
        sb.append("Monthly amount,${item.monthlyAmount}\n")
        if (item.interestAmount > 0) sb.append("Interest,${item.interestAmount}\n")
        sb.append("Total paid,${plan.totalPaid}\n")
        sb.append("Remaining amount,${plan.remainingAmount}\n")
        sb.append("Paid installments,${plan.paidInstallments}\n")
        sb.append("Total installments,${plan.totalInstallments}\n")
        sb.append("Completion,${(plan.completionFraction * 100).toInt()}%\n")
        if (plan.nextPaymentDate > 0) sb.append("Next payment,${df.format(Date(plan.nextPaymentDate))}\n")
        sb.append("Expected finish,${df.format(Date(plan.expectedFinishDate))}\n\n")

        sb.append("Payment history\nDate,Type,Amount,Note\n")
        payments.sortedByDescending { it.date }.forEach { p ->
            sb.append(
                "${if (p.date > 0) df.format(Date(p.date)) else ""}," +
                    "${paymentTypeLabel(p.type)},${p.amount},${esc(p.note)}\n"
            )
        }
        return sb.toString()
    }

    fun writeCsvToUri(context: Context, uri: Uri, csv: String): Boolean = try {
        context.contentResolver.openOutputStream(uri, "w")?.use { it.write(csv.toByteArray(Charsets.UTF_8)); it.flush() }
        true
    } catch (e: Exception) { false }

    fun suggestedCsvName(item: FinancialItemEntity): String =
        "Installment_${item.title.replace(Regex("[^A-Za-z0-9]"), "_").take(24)}.csv"

    private fun buildHtml(item: FinancialItemEntity, payments: List<FinancialPaymentEntity>, isArabic: Boolean): String {
        val dir = if (isArabic) "rtl" else "ltr"
        val align = if (isArabic) "right" else "left"
        val cur = item.currency
        val plan = FinancialCalculator.computePlan(item, payments)
        val rows = payments.sortedByDescending { it.date }.joinToString("") { p ->
            val date = if (p.date > 0) dfLong.format(Date(p.date)) else "-"
            val typeText = when (p.type) {
                "FULL" -> if (isArabic) "كامل" else "Full"
                "ADVANCE" -> if (isArabic) "مقدم" else "Advance"
                "EXTRA" -> if (isArabic) "إضافي" else "Extra"
                "RECURRING" -> if (isArabic) "شهري" else "Monthly"
                else -> if (isArabic) "جزئي" else "Partial"
            }
            "<tr><td>$date</td><td>$typeText</td><td>${trim(p.amount)} $cur</td><td>${esc(p.note)}</td></tr>"
        }
        val header = if (isArabic)
            "<tr><th>التاريخ</th><th>النوع</th><th>المبلغ</th><th>ملاحظة</th></tr>"
        else
            "<tr><th>Date</th><th>Type</th><th>Amount</th><th>Note</th></tr>"
        fun line(label: String, value: String) = "<tr><td>$label</td><td><b>$value</b></td></tr>"
        val plan_rows = buildString {
            append(line(if (isArabic) "إجمالي السعر" else "Total price", "${trim(item.totalPrice)} $cur"))
            append(line(if (isArabic) "المدفوع" else "Total paid", "${trim(plan.totalPaid)} $cur"))
            append(line(if (isArabic) "المتبقّي" else "Remaining", "${trim(plan.remainingAmount)} $cur"))
            append(line(if (isArabic) "الأقساط" else "Installments", "${plan.paidInstallments} / ${plan.totalInstallments}"))
            append(line(if (isArabic) "نسبة الإنجاز" else "Completion", "${(plan.completionFraction * 100).toInt()}%"))
            if (plan.nextPaymentDate > 0) append(line(if (isArabic) "الدفعة القادمة" else "Next payment", dfLong.format(Date(plan.nextPaymentDate))))
            append(line(if (isArabic) "الانتهاء المتوقّع" else "Expected finish", dfLong.format(Date(plan.expectedFinishDate))))
        }
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
            <h1>${esc(item.title)}</h1>
            <p class="sub">${if (item.brand.isNotBlank()) esc(item.brand) + " • " else ""}${if (item.store.isNotBlank()) esc(item.store) else ""}</p>
            <table>$plan_rows</table>
            <h3>${if (isArabic) "سجل الدفعات" else "Payment history"}</h3>
            <table>$header$rows</table>
            </body></html>
        """.trimIndent()
    }

    fun printInstallment(context: Context, item: FinancialItemEntity, payments: List<FinancialPaymentEntity>, isArabic: Boolean) {
        val html = buildHtml(item, payments, isArabic)
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
                val jobName = "Installment ${item.title}"
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
