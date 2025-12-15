package com.example.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchScope: SearchScope,
    onSearchScopeChange: (SearchScope) -> Unit,
    searchResults: Map<LocalDate, List<Festivo>>,
    onClose: () -> Unit,
    onEventClick: (Festivo) -> Unit,
    availableCalendars: List<CalendarInfo>
) {
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cerrar búsqueda",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 18.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimary),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Buscar eventos...", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), fontSize = 18.sp)
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar búsqueda",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                TabRow(
                    selectedTabIndex = searchScope.ordinal,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    SearchScope.values().forEach { scope ->
                        Tab(
                            selected = searchScope == scope,
                            onClick = { onSearchScopeChange(scope) },
                            text = { Text(scope.name.lowercase().replaceFirstChar { it.titlecase() }) }
                        )
                    }
                }
            }
        },
        containerColor = CalendarioTheme.colors.background
    ) { paddingValues ->
        if (searchQuery.isBlank()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Introduce un término de búsqueda", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            }
            return@Scaffold
        }

        if (searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No se encontraron resultados", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            searchResults.forEach { (groupDate, events) ->
                val headerText = when (searchScope) {
                    SearchScope.MONTH -> groupDate.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.getDefault()))
                    SearchScope.YEAR -> groupDate.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.getDefault()))
                    SearchScope.ALL -> groupDate.format(DateTimeFormatter.ofPattern("yyyy", Locale.getDefault()))
                }

                item {
                    Text(
                        text = headerText.replaceFirstChar { it.titlecase() },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(events, key = { it.id.toString() + it.startTime.toString() }) { event ->
                    SearchResultItem(event, onEventClick, availableCalendars)
                }
                item { 
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)) 
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(event: Festivo, onEventClick: (Festivo) -> Unit, availableCalendars: List<CalendarInfo>) {
    val calendarColor = availableCalendars.find { it.id == event.calendarId }?.color?.let { Color(it) }
    val eventDate = event.date.format(DateTimeFormatter.ofPattern("d"))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onEventClick(event) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
            if (calendarColor != null) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(calendarColor, CircleShape)
                )
                Spacer(Modifier.padding(2.dp))
            }
            Text(eventDate, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, fontWeight = FontWeight.Medium, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
            if (!event.isAllDay && event.startTime != null) {
                Text(event.startTime.format(DateTimeFormatter.ofPattern("HH:mm")), fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            }
        }
    }
}
