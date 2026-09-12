package com.example.calendario

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.LocalTime

class AlarmActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    
    private var currentEventTitle by mutableStateOf("")
    private var currentEventId by mutableLongStateOf(-1L)

    private val stopSignalReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.calendario.ALARM_STOP_SIGNAL") {
                stopAlarm()
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Configuramos la ventana ANTES de super.onCreate para máxima prioridad
        setupScreenFlags()
        
        super.onCreate(savedInstanceState)

        // Registramos el receptor de señal de parada
        val filter = IntentFilter("com.example.calendario.ALARM_STOP_SIGNAL")
        ContextCompat.registerReceiver(
            this,
            stopSignalReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        currentEventTitle = intent.getStringExtra("event_title") ?: "Evento"
        currentEventId = intent.getLongExtra("event_id", -1L)

        handleAlarmTrigger(currentEventId)

        setContent {
            val isDark = ColorUtils.calculateLuminance(CalendarioTheme.colors.settingsBackground.toArgb()) < 0.5
            CalendarioTheme(darkTheme = isDark, themeUpdateTrigger = 0) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CalendarioTheme.colors.settingsBackground,
                ) {
                    AlarmScreen(
                        title = currentEventTitle,
                        onStop = {
                            stopAlarm()
                            finish()
                        },
                        onSnooze = {
                            stopAlarm()
                            snoozeAlarm(currentEventId, currentEventTitle)
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        val newTitle = intent.getStringExtra("event_title") ?: "Evento"
        val newId = intent.getLongExtra("event_id", -1L)
        
        LogCollector.addLog("ALARMA: Recibida nueva alarma en cola: '$newTitle'")
        
        currentEventTitle = newTitle
        currentEventId = newId
        
        handleAlarmTrigger(newId)
    }

    private fun handleAlarmTrigger(eventId: Long) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(eventId.toInt())
        
        if ((ringtone == null) || (!ringtone!!.isPlaying)) {
            startAlarm()
        }
    }

    private fun setupScreenFlags() {
        // Métodos modernos (API 27+)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        
        // Flags de ventana para forzar la visibilidad sobre el Keyguard
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
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

    private fun snoozeAlarm(eventId: Long, title: String) {
        AlarmUtils.scheduleSnooze(this, eventId, title)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(stopSignalReceiver)
        } catch (_: Exception) {}
        stopAlarm()
        super.onDestroy()
    }
}

@Composable
fun AlarmScreen(title: String, onStop: () -> Unit, onSnooze: () -> Unit) {
    val timeFormatter = AppFormats.TimeShort
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CalendarioTheme.colors.textSystem),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(id = R.string.snooze_alarm), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onStop,
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(id = R.string.stop_alarm), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
