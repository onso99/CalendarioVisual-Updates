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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBackPress: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayuda", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = CalendarioTheme.colors.settingsBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionTitle("Navegación principal")
            HelpText("- En el calendario Mensual el botón \"Mes\" cambia a Calendario Anual.")
            HelpText("- El cambio de mes o de año se hace deslizando a izquierda y a derecha.")
            HelpText("- Pulsando sobre el Año se podrá elegir el año a mostrar.")
            HelpText("- El icono Casa vuelve al mes actual.")
            HelpText("- El botón Pendientes cambia entre mostrar los eventos pendientes o Todos los eventos del mes.")

            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle("Gestión de Eventos")
            HelpText("- El botón (+) crea un nuevo evento en la fecha actual.")
            HelpText("- Pulsa en un día específico del calendario y se creará un nuevo evento para ese día. Si el día tiene eventos los mostrará también.")
            HelpText("- Pulsando sobre un evento de la lista de eventos se podrá Editar/Eliminar.")
            HelpText("- Sólo se permite la gestión de eventos en los calendarios Editables.")

            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle("Otras Funcionalidades")
            HelpText("- El menú 'Calendarios' permite seleccionar los calendarios de Google que se mostrarán.")
            HelpText("- El menú 'Opciones' permite cambiar entre modo claro/oscuro y configurar el widget.")
            HelpText("- Dentro de 'Opciones', 'Personalizar colores del tema' abre un editor avanzado para cambiar la apariencia de la app, importar/exportar o restaurar temas.")

        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun HelpText(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
