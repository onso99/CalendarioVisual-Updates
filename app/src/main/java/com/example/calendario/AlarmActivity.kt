package com.example.calendario

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupScreenFlags()

        val eventTitle = intent.getStringExtra("event_title") ?: "Evento"
        val eventId = intent.getLongExtra("event_id", -1L)

        // Cancelamos la notificación inmediatamente para que su sonido se detenga
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(eventId.toInt())

        startAlarm()

        setContent {
            val isDark = ColorUtils.calculateLuminance(CalendarioTheme.colors.settingsBackground.toArgb()) < 0.5
            CalendarioTheme(darkTheme = isDark, themeUpdateTrigger = 0) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CalendarioTheme.colors.settingsBackground,
                ) {
                    AlarmScreen(
                        title = eventTitle,
                        onStop = {
                            stopAlarm()
                            finish()
                        },
                        onSnooze = {
                            stopAlarm()
                            snoozeAlarm(eventId, eventTitle)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun setupScreenFlags() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.requestDismissKeyguard(this, null)
    }

    private fun startAlarm() {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) 
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        
        ringtone = RingtoneManager.getRingtone(this, alarmUri).apply {
            isLooping = true
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            play()
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 500, 500)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun stopAlarm() {
        ringtone?.stop()
        vibrator?.cancel()
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun snoozeAlarm(eventId: Long, title: String) {
        val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000)
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("event_id", eventId)
            putExtra("event_title", title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Al posponer, también usamos setAlarmClock para que se vea el icono de alarma
        val showIntent = Intent(this, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            this,
            eventId.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val info = AlarmManager.AlarmClockInfo(snoozeTime, showPendingIntent)
        alarmManager.setAlarmClock(info, pendingIntent)
        
        LogCollector.addLog("ALARMA: Pospuesta 10 min para '$title'")
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}

@Composable
fun AlarmScreen(title: String, onStop: () -> Unit, onSnooze: () -> Unit) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val currentTime = remember { LocalTime.now().format(timeFormatter) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentTime,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = CalendarioTheme.colors.textSystem,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = CalendarioTheme.colors.textSystem,
                lineHeight = 32.sp,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Button(
                onClick = onStop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = CircleShape,
            ) {
                Text("DETENER", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CalendarioTheme.colors.textSystem),
                shape = CircleShape,
            ) {
                Text("POSPONER (10 min)", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
