package com.notification.app.domain.calculator

import android.content.Context
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.notification.app.data.local.entities.Gam3iyaEntity
import com.notification.app.data.local.entities.Gam3iyaMemberEntity
import com.notification.app.data.local.entities.Gam3iyaPaymentEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sprint 4 — export a gam3iya as CSV (saved via SAF) or as a printable /
 * PDF document (system print dialog → print or "Save as PDF"). Text sharing
 * stays in [StatementExporter].
 */
object Gam3iyaExporter {

    private val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    // Keep a strong ref so the WebView isn't GC'd before the print spooler runs.
    private var printWebView: WebView? = null

    fun buildCsv(gam3iya: Gam3iyaEntity, members: List<Gam3iyaMemberEntity>, payments: List<Gam3iyaPaymentEntity>): String {
        val sb = StringBuilder()
        sb.append("Gam3iya,${esc(gam3iya.title)}\n")
        sb.append("Mode,${gam3iya.mode}\n")
        sb.append("Currency,${gam3iya.currency}\n")
        sb.append("Monthly installment,${gam3iya.monthlyInstallment}\n")
        sb.append("Start date,${if (gam3iya.startDate > 0) df.format(Date(gam3iya.startDate)) else ""}\n\n")

        if (gam3iya.mode == "PARTICIPANT") {
            sb.append("Organizer,${esc(gam3iya.organizerName)}\n")
            sb.append("Organizer phone,${esc(gam3iya.organizerPhone)}\n")
            sb.append("My turn,${gam3iya.myTurnNumber}\n")
            sb.append("My installment,${gam3iya.myInstallmentAmount}\n")
            sb.append("Paid installments,${gam3iya.myPaidInstallments}\n\n")
        } else {
            sb.append("Turn,Name,Phone,WhatsApp,Collection date,Installment,Paid,Collected,Late\n")
            members.sortedBy { it.turnMonth }.forEach { m ->
                sb.append(
                    "${m.turnMonth},${esc(m.memberName)},${esc(m.phone)},${esc(m.whatsapp)}," +
                        "${if (m.payoutDate > 0) df.format(Date(m.payoutDate)) else ""}," +
                        "${if (m.installmentAmount > 0) m.installmentAmount else gam3iya.monthlyInstallment}," +
                        "${if (m.isInstallmentPaidThisMonth) "yes" else "no"}," +
                        "${if (m.isPayoutReceived) "yes" else "no"}," +
                        "${if (m.isLate) "yes" else "no"}\n"
                )
            }
        }
        if (payments.isNotEmpty()) {
            sb.append("\nPayment history\nType,Month,Amount,Date\n")
            payments.sortedByDescending { it.date }.forEach { p ->
                sb.append("${p.type},${p.monthIndex},${p.amount},${if (p.date > 0) df.format(Date(p.date)) else ""}\n")
            }
        }
        return sb.toString()
    }

    /** Writes CSV text to a user-chosen SAF uri. Returns true on success. */
    fun writeCsvToUri(context: Context, uri: Uri, csv: String): Boolean = try {
        context.contentResolver.openOutputStream(uri, "w")?.use { it.write(csv.toByteArray(Charsets.UTF_8)); it.flush() }
        true
    } catch (e: Exception) { false }

    fun suggestedCsvName(gam3iya: Gam3iyaEntity): String =
        "Gam3iya_${gam3iya.title.replace(Regex("[^A-Za-z0-9]"), "_").take(24)}.csv"

    private fun buildHtml(gam3iya: Gam3iyaEntity, members: List<Gam3iyaMemberEntity>, isArabic: Boolean): String {
        val dir = if (isArabic) "rtl" else "ltr"
        val cur = gam3iya.currency
        val rows = if (gam3iya.mode == "PARTICIPANT") {
            """<tr><td>${gam3iya.myTurnNumber}</td><td>${esc(gam3iya.organizerName)}</td>
               <td>${gam3iya.myInstallmentAmount} $cur</td>
               <td>${gam3iya.myPaidInstallments}</td></tr>"""
        } else {
            members.sortedBy { it.turnMonth }.joinToString("") { m ->
                val inst = if (m.installmentAmount > 0) m.installmentAmount else gam3iya.monthlyInstallment
                val date = if (m.payoutDate > 0) df.format(Date(m.payoutDate)) else "-"
                val state = when {
                    m.isPayoutReceived -> if (isArabic) "قبض" else "Collected"
                    m.isLate -> if (isArabic) "متأخر" else "Late"
                    m.isInstallmentPaidThisMonth -> if (isArabic) "دفع" else "Paid"
                    else -> "-"
                }
                "<tr><td>${m.turnMonth}</td><td>${esc(m.memberName)}</td><td>$date</td><td>$inst $cur</td><td>$state</td></tr>"
            }
        }
        val header = if (gam3iya.mode == "PARTICIPANT") {
            if (isArabic) "<tr><th>الدور</th><th>المنظّم</th><th>القسط</th><th>مدفوع</th></tr>"
            else "<tr><th>Turn</th><th>Organizer</th><th>Installment</th><th>Paid</th></tr>"
        } else {
            if (isArabic) "<tr><th>الدور</th><th>الاسم</th><th>تاريخ القبض</th><th>القسط</th><th>الحالة</th></tr>"
            else "<tr><th>Turn</th><th>Name</th><th>Collection</th><th>Installment</th><th>Status</th></tr>"
        }
        return """
            <html dir="$dir"><head><meta charset="utf-8">
            <style>
              body{font-family:sans-serif;padding:24px;color:#111}
              h1{color:#8a6d1a}
              table{width:100%;border-collapse:collapse;margin-top:16px}
              th,td{border:1px solid #ccc;padding:8px;text-align:${if (isArabic) "right" else "left"}}
              th{background:#f3ecd6}
            </style></head><body>
            <h1>${esc(gam3iya.title)}</h1>
            <p>${if (isArabic) "القسط الشهري" else "Monthly installment"}: ${gam3iya.monthlyInstallment} $cur</p>
            <table>$header$rows</table>
            </body></html>
        """.trimIndent()
    }

    /** Opens the Android print dialog (user can print or Save as PDF). */
    fun printGam3iya(context: Context, gam3iya: Gam3iyaEntity, members: List<Gam3iyaMemberEntity>, isArabic: Boolean) {
        val html = buildHtml(gam3iya, members, isArabic)
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
                val jobName = "Gam3iya ${gam3iya.title}"
                val adapter = view.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, adapter, PrintAttributes.Builder().build())
                printWebView = null
            }
        }
        printWebView = webView
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun esc(s: String): String = s.replace("\"", "\"\"").replace("\n", " ").replace(",", " ")
}
