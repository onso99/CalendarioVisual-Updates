package com.example.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var historyText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val text = withContext(Dispatchers.IO) {
            try {
                context.assets.open("history.txt").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
        historyText = text
    }

    val showScrollToTop by remember {
        derivedStateOf { scrollState.value > 1500 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.history), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CalendarioTheme.colors.cabecera,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (showScrollToTop) {
                FloatingActionButton(
                    onClick = { scope.launch { scrollState.animateScrollTo(0) } },
                    containerColor = CalendarioTheme.colors.cabecera,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) { Icon(Icons.Default.ArrowUpward, null) }
            }
        },
        containerColor = CalendarioTheme.colors.settingsBackground
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val screenHeight = maxHeight
            val screenHeightPx = constraints.maxHeight.toFloat()
            
            // OPTIMIZACIÓN DE ANCHO: Menos relleno lateral para dar más espacio al texto
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(start = 12.dp, end = 28.dp, top = 16.dp, bottom = 16.dp)
            ) {
                Text(
                    text = historyText,
                    color = CalendarioTheme.colors.textSystem,
                    fontSize = 13.sp, // Texto reducido para mejor ajuste (v3.1.34)
                    lineHeight = 18.sp
                )
                Box(modifier = Modifier.height(120.dp))
            }

            val maxScroll = scrollState.maxValue.toFloat()
            if (maxScroll > 0) {
                val scrollbarAlpha by remember { derivedStateOf { if (scrollState.isScrollInProgress) 0.8f else 0.35f } }
                
                val handleHeightFraction = (screenHeightPx / (maxScroll + screenHeightPx)).coerceIn(0.1f, 0.9f)
                val handleHeight = screenHeight * handleHeightFraction
                val travelRange = screenHeight - handleHeight - 16.dp

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 1.dp, top = 8.dp, bottom = 8.dp)
                        .fillMaxHeight()
                        .width(30.dp) // Area de toque amplia pero visualmente invisible
                        .pointerInput(maxScroll) {
                            detectTapGestures { offset ->
                                val clickRatio = (offset.y / size.height).coerceIn(0f, 1f)
                                scope.launch { scrollState.scrollTo((clickRatio * maxScroll).toInt()) }
                            }
                        }
                        .pointerInput(maxScroll) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                val sensitivity = maxScroll / (size.height - handleHeight.toPx())
                                val nextScroll = scrollState.value + (dragAmount * sensitivity)
                                scope.launch { scrollState.scrollTo(nextScroll.toInt().coerceIn(0, maxScroll.toInt())) }
                            }
                        }
                ) {
                    // Pista visual ultra-fina
                    Box(modifier = Modifier.align(Alignment.Center).width(2.dp).fillMaxHeight().clip(CircleShape).background(CalendarioTheme.colors.textSystem.copy(alpha = 0.03f)))
                    
                    // Mango visual mÃ¡s delgado (3dp)
                    Box(
                        modifier = Modifier
                            .offset { 
                                val scrollProgress = scrollState.value.toFloat() / maxScroll
                                IntOffset(x = 0, y = (travelRange.toPx() * scrollProgress).toInt()) 
                            }
                            .height(handleHeight)
                            .width(3.dp)
                            .align(Alignment.TopCenter)
                            .clip(CircleShape)
                            .background(CalendarioTheme.colors.cabecera.copy(alpha = scrollbarAlpha))
                    )
                }
            }
        }
    }
}
