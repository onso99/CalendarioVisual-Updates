package com.example.calendario

import android.content.Context
import android.content.pm.PackageManager
import java.time.LocalDate
import java.util.Locale

object AboutInfo {
    const val LINE_3_AUTHOR = "Onso"
    
    private val RELEASE_DATE: LocalDate = LocalDate.of(2026, 9, 1)

    fun getFormattedDate(): String {
        val formatter = AppFormats.monthYear(Locale.getDefault())
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
