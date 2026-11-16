# Plan de Implementación: Unificación de onEventClickHandler para Festivos

Este plan actualiza [CalendarioScreen.kt](file:///C:/Users/onso9/Documents/AndroidStudio/Calendario/app/src/main/java/com/example/calendario/CalendarioScreen.kt) para unificar la pulsación de cualquier festivo (`event.isFromHolidaySource == true`), abriendo directamente el Gestor de Festivos (`HolidayManagerScreen`) sin mostrar el diálogo informativo intermedio.

---

## Cambios Propuestos

### 1. Actualizar `onEventClickHandler` en `CalendarioScreen.kt`
- Cambiar `if (event.calendarId == -1L && event.isFromHolidaySource)` por `if (event.isFromHolidaySource)`.

---

## Plan de Verificación

- Inspeccionar con `analyze_file`.
- Compilar con `gradle_build("app:assembleDebug")`.
- Verificar que al pulsar cualquier festivo (local o de Google) en la lista de eventos se abre directamente el Gestor de Festivos.
