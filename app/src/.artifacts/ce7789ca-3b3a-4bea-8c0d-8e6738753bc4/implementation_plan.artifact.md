# Plan de Implementación: Correcciones en RepetitionSelectionDialog

Este plan actualiza [DialogsEvents.kt](file:///C:/Users/onso9/Documents/AndroidStudio/Calendario/app/src/main/java/com/example/calendario/DialogsEvents.kt) para:
1. Formatear la fecha seleccionada en "Hasta la fecha" con el formato compacto `EEE, dd/MM/yyyy` (ej. "Mar, 15/12/2026").
2. Limpiar/reiniciar `tempUntil` y `tempCount` al seleccionar "No se repite" (`RepetitionRule.NONE`), evitando que persistan valores anteriores en pantalla.

---

## Cambios Propuestos

### 1. Actualizar `RepetitionSelectionDialog` en `DialogsEvents.kt`
- Cambiar el formateador de fecha a `DateTimeFormatter.ofPattern("EEE, dd/MM/yyyy", locale)`.
- En el `onClick` de la regla de repetición, si `rule == RepetitionRule.NONE`, limpiar `tempUntil = null` y `tempCount = ""`.

---

## Plan de Verificación

- Inspeccionar con `analyze_file`.
- Compilar con `gradle_build("app:assembleDebug")`.
- Verificar en emulador que la fecha se muestra de forma compacta y que al cambiar a "No se repite" se borra correctamente.
