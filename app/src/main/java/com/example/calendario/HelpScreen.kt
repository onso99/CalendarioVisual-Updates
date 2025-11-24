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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBackPress: () -> Unit) {

    val onBackgroundColor = if (isColorDark(CalendarioTheme.colors.settingsBackground)) Color.White else Color.Black
    val titleColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayuda", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = { IconButton(onClick = onBackPress) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.onPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = CalendarioTheme.colors.settingsBackground,
        contentColor = onBackgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            HelpSection(title = "Vistas de Calendario", titleColor = titleColor) {
                Text("La aplicación tiene dos vistas principales: mensual y anual. Puedes cambiar entre ellas pulsando en el año en la cabecera de la vista mensual.", textAlign = TextAlign.Justify)
                Spacer(modifier = Modifier.height(8.dp))
                Text("En la vista mensual, desliza a izquierda o derecha para cambiar de mes. Si te alejas del mes actual, aparecerá una flecha en la esquina superior izquierda para volver rápidamente.", textAlign = TextAlign.Justify)
            }
            HelpSection(title = "Gestión de Eventos", titleColor = titleColor) {
                Text("Pulsa en un día del calendario mensual para ver sus eventos en un diálogo. Desde ahí, puedes añadir un nuevo evento para ese día o pulsar en un evento existente para editarlo.", textAlign = TextAlign.Justify)
                Spacer(modifier = Modifier.height(8.dp))
                Text("También puedes crear un evento pulsando en el icono '+' en la barra superior o en un día vacío del calendario.", textAlign = TextAlign.Justify)
            }
            HelpSection(title = "Lista de Eventos", titleColor = titleColor) {
                Text("Debajo del calendario mensual se muestra una lista de los eventos del mes. Para el mes actual, puedes elegir entre ver todos los eventos o solo los pendientes (eventos futuros). Pulsa en el botón 'Todos'/'Pendientes' para cambiar.", textAlign = TextAlign.Justify)
            }
            HelpSection(title = "Personalización", titleColor = titleColor) {
                Text("Puedes personalizar completamente la apariencia de la aplicación desde el menú 'Opciones'. Esto incluye cambiar entre tema claro y oscuro, y modificar cada color de la interfaz.", textAlign = TextAlign.Justify)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Los temas de color se pueden exportar e importar como ficheros .json, lo que te permite guardar tus creaciones o compartirlas.", textAlign = TextAlign.Justify)
            }
            HelpSection(title = "Widget", titleColor = titleColor) {
                Text("La aplicación incluye un widget para tu pantalla de inicio que muestra los próximos eventos. Puedes personalizar su apariencia (número de eventos, tamaño de letra y colores) desde la pantalla de 'Opciones'.", textAlign = TextAlign.Justify)
            }
        }
    }
}

@Composable
private fun HelpSection(title: String, titleColor: Color, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}