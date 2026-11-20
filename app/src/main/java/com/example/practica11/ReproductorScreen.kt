package com.example.practica11

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReproductorScreen() {

    // Estado de navegación entre las secciones de la aplicación
    var selectedTab by remember { mutableIntStateOf(0) }

    // Configuración del tema y los colores de la interfaz
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF7045A1),
            primaryContainer = Color(0xFFEADDFF),
            secondaryContainer = Color(0xFFE8DEF8)
        )
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Mini Reproductor MP3") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {

                // Pestañas de navegación entre reproductor y archivos de texto
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Reproductor") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Archivos de texto") }
                    )
                }

                // Visualización de la sección seleccionada
                when (selectedTab) {
                    0 -> ReproductorContenido()
                    else -> ArchivoScreen()
                }
            }
        }
    }
}

@Composable
private fun ReproductorContenido() {
    val context = LocalContext.current

    // Estados del archivo de audio y del reproductor
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var audioName by remember {
        mutableStateOf("Ningún archivo seleccionado")
    }
    var status by remember { mutableStateOf("") }
    var playbackState by remember { mutableStateOf("Sin reproducción") }
    var positionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }

    // Selección de archivos de audio mediante el explorador de Android
    val selectorAudio = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {

            // Liberación del reproductor anterior y reinicio de los estados
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
            audioUri = uri
            audioName = nombreDeArchivo(context, uri)
            positionMs = 0
            durationMs = 0
            playbackState = "Sin reproducción"

            // Carga e inicio de reproducción del archivo seleccionado
            runCatching {
                MediaPlayer.create(context, uri)
                    ?: throw IllegalStateException("Archivo incompatible")
            }.onSuccess { player ->
                mediaPlayer = player
                durationMs = player.duration.coerceAtLeast(0)

                // Actualización del estado al finalizar la pista
                player.setOnCompletionListener {
                    isPlaying = false
                    positionMs = durationMs
                    playbackState = "Finalizado"
                }

                player.start()
                isPlaying = true
                playbackState = "Reproduciendo"
                status = "Audio cargado correctamente"

            }.onFailure {
                audioUri = null
                playbackState = "Sin reproducción"
                status = "No se pudo abrir este archivo de audio"
            }
        }
    }

    // Actualización periódica del progreso durante la reproducción
    LaunchedEffect(mediaPlayer, isPlaying) {
        while (isPlaying) {
            val player = mediaPlayer ?: break

            positionMs = runCatching {
                player.currentPosition.coerceIn(0, durationMs)
            }.getOrDefault(positionMs)

            delay(250)
        }
    }

    // Liberación de recursos al cerrar la pantalla del reproductor
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Interfaz principal del reproductor MP3
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Nombre del archivo de audio seleccionado
        Text(
            text = if (audioUri == null)
                "Ningún archivo seleccionado"
            else audioName,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón para seleccionar archivos de audio del dispositivo
        Button(onClick = { selectorAudio.launch("audio/*") }) {
            Text("Buscar MP3 en SD/Memoria")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mensajes de confirmación y errores del reproductor
        if (status.isNotEmpty()) {
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Barra de progreso con posibilidad de cambiar la posición del audio
        Slider(
            value = positionMs.toFloat().coerceIn(
                0f,
                durationMs.coerceAtLeast(1).toFloat()
            ),
            onValueChange = { value ->
                mediaPlayer?.let { player ->
                    val target = value.toInt().coerceIn(0, durationMs)

                    runCatching {
                        player.seekTo(target)
                        positionMs = target
                    }
                }
            },
            valueRange = 0f..durationMs.coerceAtLeast(1).toFloat(),
            enabled = mediaPlayer != null && durationMs > 0,
            modifier = Modifier.fillMaxWidth()
        )

        // Indicadores del tiempo transcurrido y duración total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatoTiempo(positionMs),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                formatoTiempo(durationMs),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Controles principales de reproducción, pausa y detención
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Reproducción o reanudación desde la posición actual
            FloatingActionButton(
                onClick = {
                    mediaPlayer?.let { player ->
                        runCatching {
                            if (!player.isPlaying) {
                                if (player.currentPosition >= player.duration) {
                                    player.seekTo(0)
                                    positionMs = 0
                                }

                                player.start()
                                isPlaying = true
                                playbackState = "Reproduciendo"
                            }
                        }.onFailure {
                            status = "No se pudo reproducir el audio"
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Reproducir",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Pausa del audio conservando la posición de reproducción
            FloatingActionButton(
                onClick = {
                    mediaPlayer?.let { player ->
                        if (isPlaying) {
                            runCatching {
                                player.pause()
                                positionMs = player.currentPosition.coerceIn(
                                    0,
                                    durationMs
                                )
                                isPlaying = false
                                playbackState = "Pausado"
                            }.onFailure {
                                status = "No se pudo pausar el audio"
                            }
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Pause,
                    contentDescription = "Pausar",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Detención del audio y reinicio de la pista al segundo cero
            FloatingActionButton(
                onClick = {
                    mediaPlayer?.let { player ->
                        runCatching {
                            player.pause()
                            player.seekTo(0)
                            positionMs = 0
                            isPlaying = false
                            playbackState = "Detenido"
                        }.onFailure {
                            status = "No se pudo detener el audio"
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = "Detener",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visualización del estado actual de reproducción
        Text(
            text = playbackState,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

// Conversión de milisegundos al formato de minutos y segundos
private fun formatoTiempo(milliseconds: Int): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return String.format(
        Locale.US,
        "%02d:%02d",
        seconds / 60,
        seconds % 60
    )
}

// Obtención del nombre del archivo mediante su identificador URI
private fun nombreDeArchivo(context: Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(
                OpenableColumns.DISPLAY_NAME
            )

            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getString(index)
            } else {
                null
            }
        }
    }.getOrNull() ?: "Audio seleccionado"
}
