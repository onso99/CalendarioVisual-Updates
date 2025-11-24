package com.example.calendario

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log

object CalendarObserverManager {
    private const val TAG = "CalendarObserverManager"

    @SuppressLint("StaticFieldLeak")
    private var calendarObserverInstance: CalendarObserver? = null

    fun registerObserver(context: Context) {
        if (calendarObserverInstance == null) {
            Log.d(TAG, "Creando nueva instancia de CalendarObserver.")
            calendarObserverInstance = CalendarObserver(context.applicationContext)
            calendarObserverInstance?.register()
        } else {
            Log.w(TAG, "Register llamado pero la instancia ya existía. Re-registrando.")
            calendarObserverInstance?.register() // Volver a registrar por si acaso
        }
    }

    fun unregisterObserver() {
        if (calendarObserverInstance != null) {
            Log.d(TAG, "Desregistrando la instancia de CalendarObserver.")
            calendarObserverInstance?.unregister()
            calendarObserverInstance = null
        } else {
            Log.w(TAG, "Unregister llamado pero no había instancia registrada.")
        }
    }
}