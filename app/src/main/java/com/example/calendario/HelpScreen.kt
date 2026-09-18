package com.example.calendario

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme

@Composable
fun HelpScreen(onBackPress: () -> Unit) {
    AppScreen(
        title = stringResource(id = R.string.help),
        onBackClick = onBackPress
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            HelpSection(title = stringResource(id = R.string.help_section_calendar_views)) {
                Text(stringResource(id = R.string.help_calendar_views_1), fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(id = R.string.help_calendar_views_2), fontSize = 14.sp, lineHeight = 20.sp)
            }
            HelpSection(title = stringResource(id = R.string.help_section_event_management)) {
                Text(stringResource(id = R.string.help_event_management_1), fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(id = R.string.help_event_management_2), fontSize = 14.sp, lineHeight = 20.sp)
            }
            HelpSection(title = stringResource(id = R.string.help_section_event_list)) {
                Text(stringResource(id = R.string.help_event_list_1), fontSize = 14.sp, lineHeight = 20.sp)
            }
            HelpSection(title = stringResource(id = R.string.help_section_customization)) {
                Text(stringResource(id = R.string.help_customization_1), fontSize = 14.sp, lineHeight = 20.sp)
            }
            HelpSection(title = stringResource(id = R.string.help_section_holiday_manager)) {
                Text(stringResource(id = R.string.help_holiday_manager_1), fontSize = 14.sp, lineHeight = 20.sp)
            }
            HelpSection(title = stringResource(id = R.string.help_section_widget)) {
                Text(stringResource(id = R.string.help_widget_1), fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun HelpSection(title: String, content: @Composable () -> Unit) {
    val titleColor = lerp(
        start = CalendarioTheme.colors.cabecera,
        stop = CalendarioTheme.colors.textSystem,
        fraction = 0.4f
    )

    Column(modifier = Modifier.padding(bottom = 32.dp)) { // Aumentado a 32dp para uniformidad
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            modifier = Modifier
                .padding(bottom = 2.dp) // Reducido a 2dp para consistencia (v3.2.12.5)
                .offset(y = (-8).dp) // COMPENSACIÃ“N VISUAL UNIFICADA
        )
        content()
    }
}
