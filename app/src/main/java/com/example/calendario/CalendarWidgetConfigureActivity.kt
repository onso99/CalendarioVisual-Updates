package com.example.calendario // ★★★ REEMPLAZA con tu nombre de paquete real ★★★

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

class CalendarWidgetConfigureActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_configure)

        intent.extras?.also {
            appWidgetId = it.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.settings_container, SettingsFragment())
                commit()
            }
        }

        setResult(Activity.RESULT_CANCELED)

        onBackPressedDispatcher.addCallback(this) {
            finishConfiguration()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.calendar_widget_preferences, rootKey)
        }
    }

    private fun finishConfiguration() {
        val appWidgetManager = AppWidgetManager.getInstance(this)

        // ★★★ CORRECCIÓN APLICADA AQUÍ ★★★
        val idDeTuListViewEnElLayoutPrincipalDelWidget = R.id.widget_event_list // <--- CAMBIADO
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, idDeTuListViewEnElLayoutPrincipalDelWidget)

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}
