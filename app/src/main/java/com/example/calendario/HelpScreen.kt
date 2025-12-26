package com.example.calendario

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBackPress: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.help)) },
                navigationIcon = { IconButton(onClick = onBackPress) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back)) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CalendarioTheme.colors.cabecera,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = CalendarioTheme.colors.settingsBackground,
        contentColor = CalendarioTheme.colors.textSystem
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp) 
        ) {
            HelpSection(title = stringResource(id = R.string.help_section_calendar_views)) {
                Text(stringResource(id = R.string.help_calendar_views_1))
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(id = R.string.help_calendar_views_2))
            }
            HelpSection(title = stringResource(id = R.string.help_section_event_management)) {
                Text(stringResource(id = R.string.help_event_management_1))
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(id = R.string.help_event_management_2))
            }
            HelpSection(title = stringResource(id = R.string.help_section_event_list)) {
                Text(stringResource(id = R.string.help_event_list_1))
            }
            HelpSection(title = stringResource(id = R.string.help_section_customization)) {
                Text(stringResource(id = R.string.help_customization_1))
            }
            HelpSection(title = stringResource(id = R.string.help_section_widget)) {
                Text(stringResource(id = R.string.help_widget_1))
            }
        }
    }
}

@Composable
private fun HelpSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CalendarioTheme.colors.cabecera,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}
