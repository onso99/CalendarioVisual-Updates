# Plan de Implementación: Sincronización del Switch en HolidayManagerScreen

Este plan actualiza [HolidayManagerScreen.kt](file:///C:/Users/onso9/Documents/AndroidStudio/Calendario/app/src/main/java/com/example/calendario/HolidayManagerScreen.kt) para que, al abrir el gestor de festivos con un festivo existente (`initialFestivo`), se compruebe si ya existe un `HolidayAdjustment` guardado para esa fecha y se inicialice correctamente el estado del switch (`isHoliday`) reflejando si es festivo o laborable.

---

## Cambios Propuestos

### 1. Actualizar `HolidayManagerScreen.kt`
- Consultar si existe un ajuste previo para `initialFestivo?.date`.
- Inicializar `isHoliday` y `refIsHoliday` usando el tipo del ajuste existente (`type == HolidayAdjustmentType.HOLIDAY`) si está presente, o `initialFestivo?.isFromHolidaySource ?: true` en su defecto.

---

## Plan de Verificación

- Inspeccionar con `analyze_file`.
- Compilar con `gradle_build("app:assembleDebug")`.
- Verificar en pantalla que al abrir el gestor de festivos para un festivo marcado como laborable, el switch aparece desactivado (laborable).
