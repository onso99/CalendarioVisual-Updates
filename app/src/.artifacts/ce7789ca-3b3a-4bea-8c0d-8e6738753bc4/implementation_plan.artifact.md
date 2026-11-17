# Plan de Implementación: Nomenclatura Minúscula para Composables que Retornan Valores

Este plan actualiza [CommonComponents.kt](file:///C:/Users/onso9/Documents/AndroidStudio/Calendario/app/src/main/java/com/example/calendario/CommonComponents.kt), [SettingsScreen.kt](file:///C:/Users/onso9/Documents/AndroidStudio/Calendario/app/src/main/java/com/example/calendario/SettingsScreen.kt), [HolidayManagerScreen.kt](file:///C:/Users/onso9/Documents/AndroidStudio/Calendario/app/src/main/java/com/example/calendario/HolidayManagerScreen.kt) y [ColorThemeScreen.kt](file:///C:/Users/onso9/Documents/AndroidStudio/Calendario/app/src/main/java/com/example/calendario/ColorThemeScreen.kt) para renombrar `AppSwitchColors` a `appSwitchColors` y `AppSliderColors` a `appSliderColors`.

---

## Cambios Propuestos

### 1. Actualizar `CommonComponents.kt`
- Renombrar funciones a minúscula inicial.

### 2. Actualizar llamadas en `SettingsScreen.kt`, `HolidayManagerScreen.kt` y `ColorThemeScreen.kt`
- Actualizar referencias a `appSwitchColors()` y `appSliderColors()`.

---

## Plan de Verificación

- Inspeccionar con `analyze_file`.
- Compilar con `gradle_build("app:assembleDebug")`.
- Verificar compilación limpia sin warnings de nomenclatura de `@Composable`.
