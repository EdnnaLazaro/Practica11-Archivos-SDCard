package com.example.practica11

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

internal enum class DestinoArchivo {
    INTERNO,
    EXTERNO
}

internal class ArchivoRepository(private val context: Context) {
    fun guardar(destino: DestinoArchivo, texto: String): File {
        val archivo = obtenerArchivo(destino)
        archivo.parentFile?.mkdirs()
        FileOutputStream(archivo, false).use { salida ->
            salida.write(texto.toByteArray(Charsets.UTF_8))
        }
        return archivo
    }

    fun leer(destino: DestinoArchivo): String {
        val archivo = obtenerArchivo(destino)
        if (!archivo.exists()) {
            throw IOException("Todavía no existe un archivo en esta ubicación")
        }
        return FileInputStream(archivo).bufferedReader(Charsets.UTF_8).use { entrada ->
            entrada.readText()
        }
    }

    fun descripcionExterna(): String {
        val directorio = obtenerDirectorioExterno()
        return if (Environment.isExternalStorageRemovable(directorio)) {
            "Tarjeta SDCard"
        } else {
            "Memoria externa del dispositivo"
        }
    }

    private fun obtenerArchivo(destino: DestinoArchivo): File {
        val directorio = when (destino) {
            DestinoArchivo.INTERNO -> context.filesDir
            DestinoArchivo.EXTERNO -> obtenerDirectorioExterno()
        }
        return File(directorio, "practica11.txt")
    }

    private fun obtenerDirectorioExterno(): File {
        val directorios = context.getExternalFilesDirs(null)?.filterNotNull().orEmpty()
        return directorios.firstOrNull { directorio ->
            runCatching { Environment.isExternalStorageRemovable(directorio) }.getOrDefault(false)
        } ?: context.getExternalFilesDir(null)
        ?: throw IOException("El almacenamiento externo no está disponible")
    }
}
