package com.example.calendario // O tu paquete correspondiente

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Comprueba si un calendario específico tiene al menos un evento "visible"
 * dentro de un rango de tiempo.
 *
 * @param context El contexto de la aplicación.
 * @param calendarId El ID del calendario a verificar.
 * @return `true` si se encuentra al menos un evento que cumpla los criterios, `false` en caso contrario o si hay error.
 */
suspend fun hasVisibleEvents(context: Context, calendarId: Long): Boolean {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        Log.w("CalendarCheck", "hasVisibleEvents: Permiso de lectura de calendario no concedido para verificar ID $calendarId.")
        return false
    }

    return withContext(Dispatchers.IO) {
        var eventFound = false
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE, // Aún útil si un error ocurre al leerlo
            CalendarContract.Events.VISIBLE
        )

        // Rango de fechas para la búsqueda: desde 1 año atrás hasta 2 años en el futuro desde el inicio del año actual.
        val nowZoned = ZonedDateTime.now(ZoneId.systemDefault())
        val startRangeMillis = nowZoned.minusYears(1).withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
            .toInstant().toEpochMilli()
        val endRangeMillis = nowZoned.plusYears(2).withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
            .toInstant().toEpochMilli()

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startRangeMillis)
        ContentUris.appendId(builder, endRangeMillis)
        val eventsUri = builder.build()

        val selection = "${CalendarContract.Instances.CALENDAR_ID} = ? AND ${CalendarContract.Instances.VISIBLE} = ?"
        val selectionArgs = arrayOf(calendarId.toString(), "1") // 1 para visible

        try {
            context.contentResolver.query(
                eventsUri,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                // Log.d("CalendarCheck", "Calendar ID $calendarId: Cursor de instancias de eventos tiene ${cursor.count} entradas (filtrado por visible=1).") // Comentado/Eliminado

                if (cursor.moveToFirst()) {
                    // Si solo necesitamos saber si existe *alguno*, no necesitamos leer los detalles aquí
                    // a menos que sea para un log de depuración muy específico.
                    // val eventId = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events._ID))
                    // val title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE))
                    // val visibleFlag = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Events.VISIBLE))
                    // Log.d("CalendarCheck", "Calendar ID $calendarId: Encontrado evento (ID: $eventId, Título: '$title', VisibleFlag: $visibleFlag). Devolviendo true.") // Comentado/Eliminado
                    eventFound = true
                }
            } ?: Log.w("CalendarCheck", "Calendar ID $calendarId: El cursor de instancias de eventos es nulo.") // Mantener: importante si la query falla
        } catch (e: SecurityException) {
            Log.e("CalendarCheck", "hasVisibleEvents: Excepción de seguridad para ID $calendarId - ${e.message}", e) // Mantener
            return@withContext false
        } catch (e: Exception) {
            Log.e("CalendarCheck", "hasVisibleEvents: Error general para ID $calendarId - ${e.message}", e) // Mantener
            return@withContext false
        }

        // if (!eventFound) {
        // Log.d("CalendarCheck", "Calendar ID $calendarId: No se encontraron eventos visibles. Devolviendo false.") // Comentado/Eliminado
        // }
        eventFound
    }
}
