# Plan de Implementación: Color de Cabecera en Icono de Banner de HolidayManagerScreen

Este plan actualiza [HolidayManagerScreen.kt](file:///C:/Users/onso9/Documents/AndroidStudio/Calendario/app/src/main/java/com/example/calendario/HolidayManagerScreen.kt) para usar `bannerIconColor = CalendarioTheme.colors.cabecera`, unificándolo con el estilo de la pantalla de Calendarios.

---

## Cambios Propuestos

### 1. Actualizar `HolidayManagerScreen.kt`
- Cambiar `bannerIconColor = festivoColor` por `bannerIconColor = CalendarioTheme.colors.cabecera`.

---

## Plan de Verificación

- Inspeccionar con `analyze_file`.
- Compilar con `gradle_build("app:assembleDebug")`.
- Verificar en pantalla que el icono del banner usa el color de cabecera del tema activo.
