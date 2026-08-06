package com.notification.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.notification.app.R

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    // Flashlight strobe — a real alarm demands attention even face-down.
    private var cameraManager: CameraManager? = null
    private var torchCameraId: String? = null
    private val flashHandler = Handler(Looper.getMainLooper())
    private var torchOn = false
    private val flashRunnable = object : Runnable {
        override fun run() {
            toggleTorch(!torchOn)
            flashHandler.postDelayed(this, 600)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_ALARM) {
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra("EXTRA_TITLE") ?: "Alarm Ringing"
        val ringtoneUriStr = intent?.getStringExtra("EXTRA_RINGTONE_URI") ?: ""

        startForeground(NOTIFICATION_ID, createNotification(title))

        raiseAlarmVolume()
        playRingtone(ringtoneUriStr)
        startVibration()
        startFlashlight()

        return START_STICKY
    }

    /**
     * Nudge the alarm stream up so a phone left on a low alarm volume
     * still rings audibly. We never touch the ring/media streams.
     */
    private fun raiseAlarmVolume() {
        try {
            val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val current = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            // Only raise if the user left it very low; keep at least 70%.
            val target = maxOf(current, (max * 0.7f).toInt())
            if (target > current) {
                audio.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startFlashlight() {
        try {
            val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull { camId ->
                cm.getCameraCharacteristics(camId)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            cameraManager = cm
            torchCameraId = id
            flashHandler.post(flashRunnable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleTorch(on: Boolean) {
        val cm = cameraManager ?: return
        val id = torchCameraId ?: return
        try {
            cm.setTorchMode(id, on)
            torchOn = on
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopFlashlight() {
        flashHandler.removeCallbacks(flashRunnable)
        if (torchOn) toggleTorch(false)
    }

    private fun playRingtone(uriString: String) {
        try {
            stopRingtone()
            val uri = if (uriString.isNotBlank()) Uri.parse(uriString)
            else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Fallback to default notification sound
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = MediaPlayer.create(applicationContext, fallbackUri)?.apply {
                    isLooping = true
                    start()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun startVibration() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 800, 400, 800, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopRingtone() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun stopAlarm() {
        stopRingtone()
        vibrator?.cancel()
        stopFlashlight()
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    private fun createNotification(title: String): Notification {
        val channelId = "alarm_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Rafeeq — Alarm Ringing Service",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText("Alarm is active. Tap to open or swipe to dismiss.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 9999
        const val ACTION_STOP_ALARM = "com.notification.app.ACTION_STOP_ALARM"
    }
}
