# Plan de Implementación: Renombrado a "Utilidades de sistema", "Herramientas locales" y "Optimizar datos"

Este plan actualiza [BackupScreen.kt](file:///C:/Users/onso9/Documents/AndroidStudio/Calendario/app/src/main/java/com/example/calendario/BackupScreen.kt), [CalendarioScreen.kt](file:///C:/Users/onso9/Documents/AndroidStudio/Calendario/app/src/main/java/com/example/calendario/CalendarioScreen.kt) y los 12 archivos `strings.xml` para renombrar:
1. Pantalla: "Centro de datos" -> "Utilidades de sistema" (`system_utilities`).
2. Sección 2: "Base de datos" -> "Herramientas locales" (`local_tools_section`).
3. Opción: "Optimizar" -> "Optimizar datos" (`optimize_data`).

---

## Cambios Propuestos

### 1. Actualizar `strings.xml` en 12 Idiomas
- Añadir y traducir `system_utilities`, `local_tools_section` y `optimize_data`.

### 2. Actualizar `BackupScreen.kt` y `CalendarioScreen.kt`
- Usar las nuevas claves de recursos de cadena.

---

## Plan de Verificación

- Inspeccionar con `analyze_file`.
- Compilar con `gradle_build("app:assembleDebug")`.
- Verificar en pantalla que la pantalla y secciones muestran los nuevos nombres unificados.
