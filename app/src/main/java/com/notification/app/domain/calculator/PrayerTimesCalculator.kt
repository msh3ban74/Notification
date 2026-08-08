package com.notification.app.domain.calculator

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

data class PrayerTime(
    val nameEn: String,
    val nameAr: String,
    val timeFormatted: String,
    val timestamp: Long
)

/**
 * Real astronomical prayer times — no fudge factors. Computes the sun's
 * declination and equation of time for the day, then solves the standard
 * sun-angle equations for each prayer. Defaults to the Egyptian General
 * Authority of Survey convention (Fajr 19.5°, Isha 17.5°) at Cairo, and
 * honours the device's real UTC offset (including DST). Accurate to about a
 * minute for Egypt and the wider region.
 */
object PrayerTimesCalculator {

    // Egyptian General Authority of Survey.
    private const val FAJR_ANGLE = 19.5
    private const val ISHA_ANGLE = 17.5
    // Sun altitude for sunrise/sunset (accounts for refraction + solar radius).
    private const val SUNSET_ANGLE = 0.833
    // Asr shadow factor: 1 = Shafi'i/Maliki/Hanbali (the majority in Egypt).
    private const val ASR_SHADOW_FACTOR = 1.0

    fun getDailyPrayerTimes(
        date: Date = Date(),
        latitude: Double = 30.0444,
        longitude: Double = 31.2357
    ): List<PrayerTime> {
        val cal = Calendar.getInstance().apply { time = date }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        // Device UTC offset (hours) for this date — includes DST automatically.
        val tzHours = TimeZone.getDefault().getOffset(date.time) / 3_600_000.0

        // Julian day for the date, shifted to the sun's position at local noon.
        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)
        val (declination, eqTime) = sunPosition(jd)

        // Solar noon in local clock hours.
        val dhuhr = 12.0 + tzHours - longitude / 15.0 - eqTime

        fun sunAngleTime(angle: Double, afterNoon: Boolean): Double {
            val t = hourAngle(angle, latitude, declination)
            return if (afterNoon) dhuhr + t else dhuhr - t
        }

        val fajr = sunAngleTime(FAJR_ANGLE, afterNoon = false)
        val maghrib = sunAngleTime(SUNSET_ANGLE, afterNoon = true)
        val isha = sunAngleTime(ISHA_ANGLE, afterNoon = true)
        val asr = dhuhr + asrTime(ASR_SHADOW_FACTOR, latitude, declination)

        val midnight = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun stamp(hours: Double): Long = midnight + Math.round(hours * 3_600_000.0)

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        fun fmt(ts: Long) = timeFormat.format(Date(ts))

        val fajrTs = stamp(fajr)
        val dhuhrTs = stamp(dhuhr)
        val asrTs = stamp(asr)
        val maghribTs = stamp(maghrib)
        val ishaTs = stamp(isha)

        return listOf(
            PrayerTime("Fajr", "الفجر", fmt(fajrTs), fajrTs),
            PrayerTime("Dhuhr", "الظهر", fmt(dhuhrTs), dhuhrTs),
            PrayerTime("Asr", "العصر", fmt(asrTs), asrTs),
            PrayerTime("Maghrib", "المغرب", fmt(maghribTs), maghribTs),
            PrayerTime("Isha", "العشاء", fmt(ishaTs), ishaTs)
        )
    }

    /** The next upcoming prayer. When the whole day has passed, rolls over to
     *  tomorrow's Fajr instead of wrongly pointing at today's past Fajr. */
    fun getNextPrayer(
        prayers: List<PrayerTime>,
        latitude: Double = 30.0444,
        longitude: Double = 31.2357
    ): PrayerTime {
        val now = System.currentTimeMillis()
        prayers.firstOrNull { it.timestamp > now }?.let { return it }
        val tomorrow = Date(now + 24L * 3_600_000L)
        return getDailyPrayerTimes(tomorrow, latitude, longitude).first()
    }

    // ── Astronomy ──────────────────────────────────────────────────────────

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /** Returns (declination in degrees, equation of time in hours). */
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dSin(g) + 0.020 * dSin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val declination = dArcSin(dSin(e) * dSin(l))
        val ra = dArcTan2(dCos(e) * dSin(l), dCos(l)) / 15.0
        val eqTime = q / 15.0 - fixHour(ra)
        return declination to eqTime
    }

    /** Hours from noon to when the sun reaches [angle] below the horizon. */
    private fun hourAngle(angle: Double, latitude: Double, declination: Double): Double {
        val numerator = -dSin(angle) - dSin(latitude) * dSin(declination)
        val denominator = dCos(latitude) * dCos(declination)
        return dArcCos((numerator / denominator).coerceIn(-1.0, 1.0)) / 15.0
    }

    private fun asrTime(shadowFactor: Double, latitude: Double, declination: Double): Double {
        val angle = -dArcCot(shadowFactor + dTan(abs(latitude - declination)))
        return hourAngle(angle, latitude, declination)
    }

    // Degree-based trig helpers (the equations above are all in degrees).
    private fun dSin(deg: Double) = sin(Math.toRadians(deg))
    private fun dCos(deg: Double) = cos(Math.toRadians(deg))
    private fun dTan(deg: Double) = tan(Math.toRadians(deg))
    private fun dArcSin(x: Double) = Math.toDegrees(asin(x))
    private fun dArcCos(x: Double) = Math.toDegrees(acos(x))
    private fun dArcTan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun dArcCot(x: Double) = Math.toDegrees(atan2(1.0, x))

    private fun fixAngle(a: Double): Double = ((a % 360.0) + 360.0) % 360.0
    private fun fixHour(a: Double): Double = ((a % 24.0) + 24.0) % 24.0
}
