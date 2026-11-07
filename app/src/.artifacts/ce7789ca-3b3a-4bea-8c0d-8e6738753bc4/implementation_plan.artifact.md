# Plan de Implementación: Diagnóstico de Restauración de Festivos

Este plan añade trazas de depuración en [BackupManager.kt](file:///C:/Users/onso9/Documents/AndroidStudio/Calendario/app/src/main/java/com/example/calendario/BackupManager.kt) para comprobar exactamente qué JSON de festivos llega durante la restauración desde Google Drive.

---

## Cambios Propuestos

### 1. Añadir Log en `BackupManager.kt`
- Registrar en `LogCollector` si `holidaysJson` fue encontrado y qué contenido posee al restaurar.

---

## Plan de Verificación

- Inspeccionar con `analyze_file`.
- Compilar con `gradle_build("app:assembleDebug")`.
- Restaurar copia y revisar los logs de la app para ver el estado de `holidaysJson`.
