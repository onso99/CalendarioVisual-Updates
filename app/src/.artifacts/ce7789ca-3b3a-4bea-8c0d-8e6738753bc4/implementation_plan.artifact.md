# Plan de Implementación: Rediseño de la Opción Fuente en Ajustes del Widget

Este plan actualiza [SettingsScreen.kt](file:///C:/Users/onso9/Documents/AndroidStudio/Calendario/app/src/main/java/com/example/calendario/SettingsScreen.kt) para:
1. Eliminar el efecto deslizante (*slide*), abriendo `FontFamilySelectionDialog` directamente al pulsar la fila de Fuente.
2. Mostrar un resumen directo de la fuente seleccionada y estado de negrita a la derecha de la fila en `SettingsScreen.kt`.
3. Incorporar la opción **Negrita** mediante un `Switch` al final del diálogo de fuentes, separado por una línea sutil.

---

## Cambios Propuestos

### 1. Actualizar Fila y Diálogo de Fuente en `SettingsScreen.kt`
- Reemplazar la fila expandible con `showFontExpand` por una fila directa que abre `showFontFamilyDialog = true` y muestra el resumen a la derecha.
- Actualizar `FontFamilySelectionDialog` para aceptar `currentBold`, gestionar `tempBold` y mostrar el Switch de Negrita al final con un `HorizontalDivider`.

---

## Plan de Verificación

- Inspeccionar con `analyze_file`.
- Compilar con `gradle_build("app:assembleDebug")`.
- Verificar en pantalla que la opción Fuente abre el diálogo unificado con Switch de Negrita y actualiza el resumen correctamente en la fila.
