package com.example.retrofit.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProcesadorImagenes(private val contexto: Context) {

    suspend fun comprimir(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            decodificar(uri)?.let { aJpeg(it) }
        } catch (e: Throwable) {
            null
        }
    }

    private fun decodificar(uri: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val fuente = ImageDecoder.createSource(contexto.contentResolver, uri)
            ImageDecoder.decodeBitmap(fuente) { decoder, info, _ ->
                val ancho = info.size.width
                val alto = info.size.height
                val escala = min(1f, LADO_MAXIMO.toFloat() / max(ancho, alto))
                decoder.setTargetSize(
                    max(1, (ancho * escala).roundToInt()),
                    max(1, (alto * escala).roundToInt())
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            decodificarAntiguo(uri)
        }

    private fun decodificarAntiguo(uri: Uri): Bitmap? {
        val resolver = contexto.contentResolver
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null
        var muestra = 1
        while (max(limites.outWidth, limites.outHeight) / (muestra * 2) >= LADO_MAXIMO) muestra *= 2
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
    }

    private fun aJpeg(bitmap: Bitmap): ByteArray {
        val base = if (bitmap.hasAlpha()) {
            Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).also {
                Canvas(it).apply {
                    drawColor(Color.WHITE)
                    drawBitmap(bitmap, 0f, 0f, null)
                }
            }
        } else {
            bitmap
        }
        return ByteArrayOutputStream().use { salida ->
            base.compress(Bitmap.CompressFormat.JPEG, 85, salida)
            salida.toByteArray()
        }
    }

    private companion object {
        const val LADO_MAXIMO = 1280
    }
}
