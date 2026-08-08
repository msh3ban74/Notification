package com.notification.app.ui.screens

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notification.app.domain.scheduler.AlarmManagerScheduler
import com.notification.app.service.AlarmService
import com.notification.app.ui.theme.MaroonPrimary
import com.notification.app.ui.theme.NotificationTheme
import com.notification.app.ui.theme.Primary
import com.notification.app.ui.theme.PrimaryDark

class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure full screen on lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Alarm Ringing"
        val note = intent.getStringExtra("EXTRA_NOTE") ?: ""
        val category = intent.getStringExtra("EXTRA_CATEGORY") ?: "Alarm"
        val isAlarm = intent.getBooleanExtra("EXTRA_IS_ALARM", true)
        val alarmId = intent.getLongExtra("EXTRA_ALARM_ID", -1L)
        val reminderId = intent.getLongExtra("EXTRA_REMINDER_ID", -1L)
        val ringtoneUri = intent.getStringExtra("EXTRA_RINGTONE_URI") ?: ""
        val snoozeMinutes = intent.getIntExtra("EXTRA_SNOOZE_MIN", SNOOZE_MINUTES)

        val isArabic = java.util.Locale.getDefault().language == "ar"

        setContent {
            NotificationTheme(isArabic = isArabic) {
                AlarmRingingScreen(
                    title = title,
                    note = note,
                    category = category,
                    isArabic = isArabic,
                    snoozeMinutes = snoozeMinutes,
                    onDismiss = {
                        stopAlarmService()
                        finish()
                    },
                    onSnooze = {
                        stopAlarmService()
                        // Snooze actually re-schedules the SAME alert +10m.
                        AlarmManagerScheduler.snooze(
                            context = this,
                            minutes = snoozeMinutes,
                            isAlarm = isAlarm,
                            alarmId = alarmId,
                            reminderId = reminderId,
                            title = title,
                            note = note,
                            category = category,
                            ringtoneUri = ringtoneUri
                        )
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val SNOOZE_MINUTES = 10
    }

    private fun stopAlarmService() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        startService(stopIntent)
    }
}

/** Category string → a fitting glyph for the ringing screen. */
private fun iconForCategory(category: String): ImageVector = when (category.uppercase()) {
    "BILL", "MONEY" -> Icons.Default.Payments
    "APPOINTMENT" -> Icons.Default.CalendarMonth
    "MEDICINE" -> Icons.Default.Medication
    else -> Icons.Default.Alarm
}

@Composable
fun AlarmRingingScreen(
    title: String,
    note: String,
    category: String,
    isArabic: Boolean = false,
    snoozeMinutes: Int = 10,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    // Live clock — updates every second so the ringing screen shows the
    // real current time, like a professional alarm.
    val nowMillis = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0L) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            nowMillis.value = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    // Honour the phone's 24-hour setting: a device on 24h shows "20:05" with
    // no AM/PM, a 12h device shows "08:05 م". Digits follow the app language.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val is24Hour = androidx.compose.runtime.remember {
        android.text.format.DateFormat.is24HourFormat(ctx)
    }
    val clockLocale = if (isArabic) java.util.Locale("ar") else java.util.Locale.ENGLISH
    val timeFormat = androidx.compose.runtime.remember(is24Hour) {
        java.text.SimpleDateFormat(if (is24Hour) "HH:mm" else "hh:mm", clockLocale)
    }
    val ampmFormat = androidx.compose.runtime.remember(is24Hour) {
        if (is24Hour) null else java.text.SimpleDateFormat("a", clockLocale)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Vivid wake-up: a deep→electric indigo wash. White clock
                // and text stay perfectly readable on it.
                Brush.verticalGradient(
                    colors = listOf(PrimaryDark, Primary, Color(0xFF6366F1))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Large live clock.
            if (nowMillis.value > 0) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeFormat.format(java.util.Date(nowMillis.value)),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 72.sp, fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    if (ampmFormat != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ampmFormat.format(java.util.Date(nowMillis.value)),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Pulsing Animated Category Symbol
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale)
                    .background(Color.White.copy(alpha = 0.22f), shape = CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(Color.White, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForCategory(category),
                        contentDescription = "Ringing Icon",
                        tint = MaroonPrimary,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Category Badge
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = category.uppercase(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            if (note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(56.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onSnooze,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.18f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(end = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Snooze, contentDescription = "Snooze")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "غفوة $snoozeMinutes د" else "Snooze $snoozeMinutes m",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaroonPrimary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(start = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Dismiss")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (isArabic) "تم" else "Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
