# Plan de Implementación: Scroll Siempre al Festivo Más Próximo a Hoy

Este plan actualiza [HolidayManagerScreen.kt](file:///C:/Users/onso9/Documents/AndroidStudio/Calendario/app/src/main/java/com/example/calendario/HolidayManagerScreen.kt) para que el scroll automático de la lista de festivos siempre posicione la vista en el festivo más próximo a la fecha actual (`LocalDate.now()`), independientemente de si la pantalla fue abierta normalmente o pulsando un festivo.

---

## Cambios Propuestos

### 1. Actualizar `HolidayManagerScreen.kt`
- Simplificar `LaunchedEffect(sortedAdjustments)` para calcular siempre el índice del festivo más cercano a Hoy (`it.date <= today`).

---

## Plan de Verificación

- Inspeccionar con `analyze_file`.
- Compilar con `gradle_build("app:assembleDebug")`.
- Verificar en pantalla que la lista siempre se posiciona en el festivo más cercano a Hoy.
