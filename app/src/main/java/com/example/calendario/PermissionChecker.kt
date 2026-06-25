@file:Suppress("DEPRECATION")

package com.example.calendario

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.services.drive.DriveScopes
import com.google.android.gms.common.api.Scope

enum class PermissionStatus {
    GRANTED,
    DENIED
}

object PermissionChecker {

    fun getGoogleDriveStatus(context: Context): PermissionStatus {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val hasScope = account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))
        return if (hasScope) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    fun getCalendarStatus(context: Context): PermissionStatus {
        val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val hasWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        return if (hasRead && hasWrite) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    fun getNotificationsStatus(context: Context): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                PermissionStatus.GRANTED
            } else {
                PermissionStatus.DENIED
            }
        } else {
            // En versiones anteriores a Android 13 se asume concedido por defecto tras instalación
            PermissionStatus.GRANTED
        }
    }

    fun getAlarmsStatus(context: Context): PermissionStatus {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // 1. Check Exact Alarms (Android 12+)
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        // 2. Check Full Screen Intent (Android 14+)
        val canUseFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.canUseFullScreenIntent()
        } else {
            true
        }

        return if (canScheduleExact && canUseFullScreen) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }
}
