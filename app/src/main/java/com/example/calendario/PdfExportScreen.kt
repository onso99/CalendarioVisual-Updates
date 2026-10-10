package com.example.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

@Composable
fun PdfExportScreen(
    onBackPress: () -> Unit,
    eventsByDate: Map<LocalDate, List<Festivo>> = emptyMap(),
    dailyNotes: Map<String, DailyNote> = emptyMap()
) {
    val locale = LocalConfiguration.current.locales[0]

    var targetMonth by remember { mutableStateOf(YearMonth.now()) }
    var customTitleText by remember { mutableStateOf<String?>(null) }
    
    var isColorMode by remember { mutableStateOf(true) }
    var isMixedLayout by remember { mutableStateOf(false) }

    var includeEvents by remember { mutableStateOf(true) }
    var includeNotes by remember { mutableStateOf(true) }
    var showEventTimes by remember { mutableStateOf(true) }
    var showLongPeriods by remember { mutableStateOf(true) }

    val defaultMonthName = remember(targetMonth, locale) {
        val monthStr = targetMonth.month.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase(locale) }
        "$monthStr ${targetMonth.year}"
    }

    val displayTitle = customTitleText ?: defaultMonthName

    AppScreen(
        title = stringResource(id = R.string.pdf_print_title),
        onBackClick = onBackPress,
        scrollable = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = AppLayout.ScreenHorizontalPadding, end = AppLayout.ScreenHorizontalPadding, bottom = 24.dp)
        ) {
            // TARJETA DE PREVISUALIZACIÓN DE TARJETA A4
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CalendarioTheme.colors.cabecera.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CalendarioTheme.colors.fondoSecciones)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_print_outlined),
                            contentDescription = null,
                            tint = CalendarioTheme.colors.cabecera,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CalendarioTheme.colors.textSystem
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isMixedLayout) "Documento Din A4 · Formato Mixto" else "Documento Din A4 · Formato Mensual",
                        fontSize = 12.sp,
                        color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECCIÓN 1: INICIAL (SIN TÍTULO DE SECCIÓN)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CalendarioTheme.colors.fondoSecciones)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Título en primer lugar
                OutlinedTextField(
                    value = displayTitle,
                    onValueChange = { customTitleText = it },
                    label = { Text("Título del PDF") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CalendarioTheme.colors.cabecera,
                        focusedLabelColor = CalendarioTheme.colors.cabecera
                    )
                )

                // Elección del mes debajo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        targetMonth = targetMonth.minusMonths(1)
                        customTitleText = null // Actualizar título al nuevo mes
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowLeft, contentDescription = null, tint = CalendarioTheme.colors.textSystem)
                    }
                    Text(
                        text = defaultMonthName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalendarioTheme.colors.textSystem
                    )
                    IconButton(onClick = {
                        targetMonth = targetMonth.plusMonths(1)
                        customTitleText = null // Actualizar título al nuevo mes
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowRight, contentDescription = null, tint = CalendarioTheme.colors.textSystem)
                    }
                }
            }

            // SECCIÓN 2: ESTILO DE DOCUMENTO
            SectionTitle(text = "Estilo de documento")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CalendarioTheme.colors.fondoSecciones)
            ) {
                OptionRow(
                    text = "Mensual",
                    isSelected = !isMixedLayout,
                    onClick = { isMixedLayout = false }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                OptionRow(
                    text = "Mixto (vertical)",
                    isSelected = isMixedLayout,
                    onClick = { isMixedLayout = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                OptionRow(
                    text = "Color",
                    isSelected = isColorMode,
                    onClick = { isColorMode = !isColorMode },
                    trailingDotColor = if (isColorMode) CalendarioTheme.colors.cabecera else Color.Gray
                )
            }

            // SECCIÓN 3: CONTENIDO
            SectionTitle(text = "Contenido")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CalendarioTheme.colors.fondoSecciones)
            ) {
                OptionRow(
                    text = "Eventos",
                    isSelected = includeEvents,
                    onClick = { includeEvents = !includeEvents }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                OptionRow(
                    text = "Notas",
                    isSelected = includeNotes,
                    onClick = { includeNotes = !includeNotes }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                OptionRow(
                    text = "Hora de inicio",
                    isSelected = showEventTimes,
                    onClick = { showEventTimes = !showEventTimes }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                OptionRow(
                    text = "Periodos largos",
                    isSelected = showLongPeriods,
                    onClick = { showLongPeriods = !showLongPeriods }
                )
            }
        }
    }
}

@Composable
private fun OptionRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    trailingDotColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            color = CalendarioTheme.colors.textSystem
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (trailingDotColor != null) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(trailingDotColor)
                )
            }
            Box(
                modifier = Modifier.size(22.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = CalendarioTheme.colors.cabecera,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
