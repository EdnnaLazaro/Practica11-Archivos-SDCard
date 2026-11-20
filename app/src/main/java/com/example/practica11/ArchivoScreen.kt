package com.example.practica11

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.IOException


@Composable
fun ArchivoScreen() {
    val context = LocalContext.current
    val repositorio = remember(context) {
        ArchivoRepository(context.applicationContext)
    }

    // Estados de la interfaz y selección del almacenamiento
    var texto by remember { mutableStateOf("") }
    var textoLeido by remember { mutableStateOf("") }
    var destino by remember { mutableStateOf(DestinoArchivo.INTERNO) }
    var estado by remember { mutableStateOf("") }
    var accionPendiente by remember { mutableStateOf<String?>(null) }

    // Verificación de permisos según la versión de Android
    var permisoConcedido by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || tienePermisos(context)
        )
    }

    // Escritura del contenido en la ubicación seleccionada
    val guardar: () -> Unit = {
        runCatching { repositorio.guardar(destino, texto) }
            .onSuccess { archivo ->
                estado = "Archivo guardado: ${archivo.absolutePath}"
            }
            .onFailure { error ->
                estado = error.message ?: "Error al guardar el archivo"
            }
        Unit
    }

    // Lectura del archivo y actualización del contenido mostrado
    val leer: () -> Unit = {
        runCatching { repositorio.leer(destino) }
            .onSuccess { contenido ->
                textoLeido = contenido
                estado = "Archivo leído correctamente"
            }
            .onFailure { error ->
                estado = error.message ?: "Error al leer el archivo"
            }
        Unit
    }

    // Solicitud de permisos de almacenamiento en tiempo de ejecución
    val pedirPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        permisoConcedido = resultados.values.all { it }

        if (permisoConcedido) {
            when (accionPendiente) {
                "guardar" -> guardar()
                "leer" -> leer()
            }
        } else {
            estado = "No se concedieron los permisos de almacenamiento"
        }

        accionPendiente = null
    }

    // Exportación de archivos TXT mediante Storage Access Framework
    val crearDocumento = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val salida = context.contentResolver.openOutputStream(uri, "w")
                    ?: throw IOException("No se puede abrir el destino")

                salida.use {
                    it.write(texto.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                estado = "Archivo TXT exportado correctamente"
            }.onFailure { error ->
                estado = error.message ?: "Error al exportar el archivo"
            }
        }
    }

    // Apertura y lectura de documentos seleccionados por el usuario
    val abrirDocumento = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val entrada = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("No se puede abrir el archivo")

                entrada.bufferedReader(Charsets.UTF_8).use {
                    it.readText()
                }
            }.onSuccess { contenido ->
                textoLeido = contenido
                estado = "Archivo TXT abierto correctamente"
            }.onFailure { error ->
                estado = error.message ?: "Error al abrir el archivo"
            }
        }
    }

    // Ejecución de operaciones con validación de permisos
    fun ejecutar(accion: String) {
        if (
            destino == DestinoArchivo.EXTERNO &&
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            !permisoConcedido
        ) {
            accionPendiente = accion

            pedirPermisos.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        } else {
            when (accion) {
                "guardar" -> guardar()
                "leer" -> leer()
            }
        }
    }

    // Interfaz principal para gestionar archivos de texto
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Manejo de archivos de texto",
            style = MaterialTheme.typography.titleLarge
        )

        // Campo de entrada para el contenido del archivo
        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            label = { Text("Escribir texto") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6
        )

        Text(
            "Ubicación de almacenamiento",
            style = MaterialTheme.typography.titleMedium
        )

        // Selección entre almacenamiento interno y externo
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = destino == DestinoArchivo.INTERNO,
                onClick = { destino = DestinoArchivo.INTERNO }
            )
            Text("Interno")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = destino == DestinoArchivo.EXTERNO,
                onClick = { destino = DestinoArchivo.EXTERNO }
            )
            Text("Externo / SDCard")
        }

        // Identificación del almacenamiento externo disponible
        if (destino == DestinoArchivo.EXTERNO) {
            Text(
                text = runCatching {
                    repositorio.descripcionExterna()
                }.getOrDefault("Almacenamiento externo no disponible"),
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Controles para guardar y recuperar archivos
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { ejecutar("guardar") }) {
                Text("Guardar")
            }

            OutlinedButton(onClick = { ejecutar("leer") }) {
                Text("Leer")
            }
        }

        HorizontalDivider()

        Text(
            "Archivos compartidos y SDCard",
            style = MaterialTheme.typography.titleMedium
        )

        // Controles para exportar y abrir documentos TXT
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    crearDocumento.launch("practica11.txt")
                }
            ) {
                Text("Exportar TXT")
            }

            OutlinedButton(
                onClick = {
                    abrirDocumento.launch(
                        arrayOf("text/plain", "text/*")
                    )
                }
            ) {
                Text("Abrir TXT")
            }
        }

        // Mensajes de confirmación y errores de las operaciones
        if (estado.isNotBlank()) {
            Text(
                estado,
                style = MaterialTheme.typography.bodySmall
            )
        }

        HorizontalDivider()

        Text(
            "Contenido leído",
            style = MaterialTheme.typography.titleMedium
        )

        // Visualización del contenido recuperado del archivo
        OutlinedTextField(
            value = textoLeido,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 8,
            label = { Text("Resultado") }
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// Comprobación de permisos de lectura y escritura en Android
private fun tienePermisos(context: Context): Boolean {
    return context.checkSelfPermission(
        Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
}


