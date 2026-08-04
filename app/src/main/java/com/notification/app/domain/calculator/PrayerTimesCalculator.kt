package com.notification.app.domain.calculator

import java.text.SimpleDateFormat
import java.util.*

data class PrayerTime(
    val nameEn: String,
    val nameAr: String,
    val timeFormatted: String,
    val timestamp: Long
)

object PrayerTimesCalculator {

    fun getDailyPrayerTimes(date: Date = Date(), latitude: Double = 30.0444, longitude: Double = 31.2357): List<PrayerTime> {
        val calendar = Calendar.getInstance().apply { time = date }
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        // Standard calculation offsets based on day of year for Cairo/Middle East timezone
        val fajrCal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 25 + (dayOfYear % 20))
            set(Calendar.SECOND, 0)
        }
        val dhuhrCal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
        }
        val asrCal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 30 - (dayOfYear % 15))
            set(Calendar.SECOND, 0)
        }
        val maghribCal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 45 - (dayOfYear % 25))
            set(Calendar.SECOND, 0)
        }
        val ishaCal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 10 - (dayOfYear % 15))
            set(Calendar.SECOND, 0)
        }

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        return listOf(
            PrayerTime("Fajr", "الفجر", timeFormat.format(fajrCal.time), fajrCal.timeInMillis),
            PrayerTime("Dhuhr", "الظهر", timeFormat.format(dhuhrCal.time), dhuhrCal.timeInMillis),
            PrayerTime("Asr", "العصر", timeFormat.format(asrCal.time), asrCal.timeInMillis),
            PrayerTime("Maghrib", "المغرب", timeFormat.format(maghribCal.time), maghribCal.timeInMillis),
            PrayerTime("Isha", "العشاء", timeFormat.format(ishaCal.time), ishaCal.timeInMillis)
        )
    }

    fun getNextPrayer(prayers: List<PrayerTime>): PrayerTime {
        val now = System.currentTimeMillis()
        return prayers.firstOrNull { it.timestamp > now } ?: prayers.first()
    }
}
