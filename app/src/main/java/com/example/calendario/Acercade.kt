package com.example.calendario

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object AboutInfo {
    const val LINE_1 = "Calendario Visual"
    const val LINE_2 = "Gemini / Android Studio"
    const val LINE_3 = "Onso / Diciembre 2025"

    fun getVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "V${packageInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("AboutInfo", "Could not get package version name", e)
            "V N/A"
        }
    }
}
