package com.notification.app.domain.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.notification.app.R
import java.io.File
import java.io.FileOutputStream

/**
 * Share-as-image — renders a branded Rafeeq card for a debt summary and
 * opens the system share sheet, so the user can send it to the other
 * person on WhatsApp or anywhere else as a clean picture.
 *
 * Pure android.graphics drawing (no Compose): indigo→violet band, the
 * bundled premium typeface, the amount front and center, and the dates.
 */
object DebtCardImage {

    fun share(
        context: Context,
        isArabic: Boolean,
        personName: String,
        amountLine: String,     // e.g. "لك عنده ٥٠٠ ج.م" / "عليك ٥٠٠ ج.م"
        receivedLine: String?,  // e.g. "تاريخ الاستلام: ٥ أغسطس ٢٠٢٦"
        dueLine: String?        // e.g. "تاريخ السداد: ١ سبتمبر ٢٠٢٦"
    ) {
        val w = 1080
        val h = 1080
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // Canvas base
        c.drawColor(Color.parseColor("#F7F6FE"))

        // Indigo → violet header band with soft rounded bottom
        val band = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), 420f,
                intArrayOf(
                    Color.parseColor("#312E81"),
                    Color.parseColor("#4F46E5"),
                    Color.parseColor("#7C3AED")
                ),
                null, Shader.TileMode.CLAMP
            )
        }
        c.drawRoundRect(RectF(-60f, -120f, w + 60f, 400f), 90f, 90f, band)

        val bold = ResourcesCompat.getFont(context, R.font.plex_arabic_bold)
        val medium = ResourcesCompat.getFont(context, R.font.plex_arabic_medium)
        val regular = ResourcesCompat.getFont(context, R.font.plex_arabic_regular)

        fun paint(size: Float, color: Int, tf: android.graphics.Typeface?) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                typeface = tf
                textAlign = Paint.Align.CENTER
            }

        // Brand
        c.drawText(if (isArabic) "رفيق" else "Rafeeq", w / 2f, 150f, paint(84f, Color.WHITE, bold))
        c.drawText(
            if (isArabic) "ملخص دين" else "Debt summary",
            w / 2f, 235f, paint(44f, Color.argb(200, 255, 255, 255), medium)
        )

        // White content card
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(24f, 0f, 10f, Color.argb(40, 49, 46, 129))
        }
        c.drawRoundRect(RectF(80f, 330f, w - 80f, 900f), 56f, 56f, cardPaint)

        // Person
        c.drawText(personName, w / 2f, 470f, paint(72f, Color.parseColor("#1E1B4B"), bold))

        // Amount / direction
        c.drawText(amountLine, w / 2f, 590f, paint(60f, Color.parseColor("#4F46E5"), bold))

        // Dates
        var y = 700f
        receivedLine?.let {
            c.drawText(it, w / 2f, y, paint(42f, Color.parseColor("#6B7280"), regular))
            y += 80f
        }
        dueLine?.let {
            c.drawText(it, w / 2f, y, paint(42f, Color.parseColor("#6B7280"), regular))
        }

        // Footer
        c.drawText(
            if (isArabic) "أُنشئ عبر تطبيق رفيق" else "Created with Rafeeq",
            w / 2f, 1000f, paint(36f, Color.parseColor("#9CA3AF"), regular)
        )

        // Save to cache/share and open the share sheet
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "rafeeq-debt-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()

        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, if (isArabic) "مشاركة كصورة" else "Share as image")
        )
    }
}
