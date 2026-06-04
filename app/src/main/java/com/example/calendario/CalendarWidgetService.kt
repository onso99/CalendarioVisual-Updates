package com.example.calendario // Asegúrate que este es tu nombre de paquete correcto

import android.content.Intent
import android.widget.RemoteViewsService

class CalendarWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        // Aquí devolvemos una instancia de nuestra implementación de RemoteViewsFactory.
        // Le pasamos el contexto de la aplicación.
        return CalendarWidgetFactory(this.applicationContext)
    }
}