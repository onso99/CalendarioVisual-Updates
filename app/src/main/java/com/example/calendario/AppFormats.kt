package com.example.calendario

import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Centralización de formatos de fecha y hora para asegurar consistencia
 * en toda la aplicación (Fase 4 - Optimización v3.1.34)
 */
object AppFormats {
    // Patrones estándar
    val TimeShort: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val TimeWithSeconds: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    
    val DateFull: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val DateAbbr: DateTimeFormatter = DateTimeFormatter.ofPattern("d/M/yy")
    val DateDayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")
    
    // Formatos combinados
    fun dateTimeWithMinutes(locale: Locale): DateTimeFormatter = 
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", locale)
    
    fun dateTimeShort(locale: Locale): DateTimeFormatter = 
        DateTimeFormatter.ofPattern("dd/MM/yy HH:mm", locale)

    fun yearOnly(locale: Locale): DateTimeFormatter = 
        DateTimeFormatter.ofPattern("yyyy", locale)
    
    // Formatos con nombre de día/mes (Requieren Locale para traducciones)
    fun dayDateAbbr(locale: Locale): DateTimeFormatter = 
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy", locale)
        
    fun dayDateFull(locale: Locale): DateTimeFormatter = 
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale)
        
    fun monthYear(locale: Locale): DateTimeFormatter = 
        DateTimeFormatter.ofPattern("MMMM yyyy", locale)

    // Formatos técnicos para ICS/Exportación
    val IcsDateTime: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
    val IcsDate: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
}
