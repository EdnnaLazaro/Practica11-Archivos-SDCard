package com.example.practica11

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

// Tipos de almacenamiento disponibles para los archivos
internal enum class DestinoArchivo {
    INTERNO,
    EXTERNO
}

// Repositorio encargado de gestionar las operaciones con archivos de texto
internal class ArchivoRepository(private val context: Context) {

    // Guarda el contenido en un archivo de texto utilizando UTF-8
    fun guardar(destino: DestinoArchivo, texto: String): File {
        val archivo = obtenerArchivo(destino)
        archivo.parentFile?.mkdirs()

        // Escritura del archivo, reemplazando su contenido anterior
        FileOutputStream(archivo, false).use { salida ->
            salida.write(texto.toByteArray(Charsets.UTF_8))
        }

        return archivo
    }

    // Recupera el contenido del archivo almacenado
    fun leer(destino: DestinoArchivo): String {
        val archivo = obtenerArchivo(destino)

        // Verificación de la existencia del archivo
        if (!archivo.exists()) {
            throw IOException("Todavía no existe un archivo en esta ubicación")
        }

        // Lectura del contenido mediante FileInputStream
        return FileInputStream(archivo).bufferedReader(Charsets.UTF_8).use { entrada ->
            entrada.readText()
        }
    }

    // Identifica el tipo de almacenamiento externo disponible
    fun descripcionExterna(): String {
        val directorio = obtenerDirectorioExterno()

        return if (Environment.isExternalStorageRemovable(directorio)) {
            "Tarjeta SDCard"
        } else {
            "Memoria externa del dispositivo"
        }
    }

    // Determina la ruta del archivo según el almacenamiento seleccionado
    private fun obtenerArchivo(destino: DestinoArchivo): File {
        val directorio = when (destino) {
            DestinoArchivo.INTERNO -> context.filesDir
            DestinoArchivo.EXTERNO -> obtenerDirectorioExterno()
        }

        return File(directorio, "practica11.txt")
    }

    // Localiza una tarjeta SDCard o el directorio externo de la aplicación
    private fun obtenerDirectorioExterno(): File {
        val directorios = context.getExternalFilesDirs(null)
            ?.filterNotNull()
            .orEmpty()

        // Prioriza el almacenamiento extraíble cuando está disponible
        return directorios.firstOrNull { directorio ->
            runCatching {
                Environment.isExternalStorageRemovable(directorio)
            }.getOrDefault(false)
        } ?: context.getExternalFilesDir(null)
        ?: throw IOException("El almacenamiento externo no está disponible")
    }
}
