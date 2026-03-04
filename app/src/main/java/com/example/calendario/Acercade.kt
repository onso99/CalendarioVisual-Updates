package com.example.calendario

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object AboutInfo {
    const val LINE_1 = "Calendario Visual"
    const val LINE_2 = "Gemini / Android Studio"
    
    // Definimos aquí el mes y año concretos que queremos mostrar
    private val RELEASE_DATE: LocalDate = LocalDate.of(2026, 3, 1)

    fun getLine3(): String {
        // Formateador que obtiene el nombre completo del mes (MMMM) y el año (yyyy) 
        // según el idioma actual del dispositivo (Locale.getDefault())
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
        
        // Formateamos la fecha y capitalizamos la primera letra (por si el sistema la da en minúscula)
        val formattedDate = RELEASE_DATE.format(formatter).replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
        
        return "Onso / $formattedDate"
    }

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
