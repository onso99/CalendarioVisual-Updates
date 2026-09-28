package com.example.calendario

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.LocalDate
import java.time.Year
import java.util.Locale

object AboutInfo {
    const val LINE_3_AUTHOR = "Onso"
    
    private val RELEASE_DATE: LocalDate = try {
        LocalDate.parse(BuildConfig.GIT_COMMIT_DATE)
    } catch (_: Exception) {
        LocalDate.of(2026, 9, 1)
    }

    fun getFormattedDate(): String {
        val formatter = AppFormats.monthYear(Locale.getDefault())
        return RELEASE_DATE.format(formatter).replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
    }

    fun getVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${packageInfo.versionName}"
        } catch (_: PackageManager.NameNotFoundException) {
            "v N/A"
        }
    }
}

@Composable
fun AboutDetailDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val currentYear = remember { Year.now().value }
    val versionName = remember { AboutInfo.getVersionName(context) }
    val displayVersion = remember { "Versión ${versionName.removePrefix("v")}" }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = android.R.string.ok),
                    color = CalendarioTheme.colors.cabecera,
                )
            }
        },
        containerColor = CalendarioTheme.colors.settingsBackground,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Icono del camaleón con fondo circular blanco ajustado (tanto en modo claro como oscuro)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White, CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.requiredSize(82.dp)
                    )
                }

                // 2. Título "Calendario Visual"
                Text(
                    text = "Calendario Visual",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CalendarioTheme.colors.textSystem,
                    textAlign = TextAlign.Center
                )

                // 3. Texto mucho más pequeño: "Versión [número]"
                Text(
                    text = displayVersion,
                    fontSize = 12.sp,
                    color = CalendarioTheme.colors.textSystem.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                // 4. Texto: "Onso [año en curso]" (tamaño reducido 13sp y peso Normal)
                Text(
                    text = "Onso $currentYear",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = CalendarioTheme.colors.textSystem,
                    textAlign = TextAlign.Center
                )

                // 5. Icono de correo electrónico sin fondo ni relleno
                IconButton(
                    onClick = {
                        val recipient = "calendariovisual26@gmail.com"
                        val subject = "Calendario Visual $versionName"
                        val mailtoUri = "mailto:$recipient?subject=${Uri.encode(subject)}".toUri()
                        val emailIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                        }
                        try {
                            context.startActivity(emailIntent)
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                "No se encontró ninguna aplicación de correo",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = "Enviar Correo",
                        tint = CalendarioTheme.colors.textSystem,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    )
}
