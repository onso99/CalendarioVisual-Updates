package com.example.calendario

import android.content.Context
import android.content.pm.PackageManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object AboutInfo {
    const val LINE_2 = "Android Studio"
    const val LINE_3_AUTHOR = "Onso"
    const val URL_HISTORIAL = "http://calendario.onso.es"
    
    private val RELEASE_DATE: LocalDate = LocalDate.of(2026, 7, 1)

    fun getFormattedDate(): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
        return RELEASE_DATE.format(formatter).replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
    }

    fun getVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${packageInfo.versionName}"
        } catch (_: PackageManager.NameNotFoundException) {
            "v N/A"
        }
    }
}
