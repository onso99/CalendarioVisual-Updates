package com.example.calendario

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme

@Composable
fun PdfExportScreen(
    onBackPress: () -> Unit
) {
    AppScreen(
        title = stringResource(id = R.string.pdf_print_title),
        onBackClick = onBackPress,
        scrollable = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_print_outlined),
                    contentDescription = null,
                    tint = CalendarioTheme.colors.cabecera,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.pdf_print_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CalendarioTheme.colors.textSystem
                )
            }
        }
    }
}
