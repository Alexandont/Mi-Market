package com.example.retrofit.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatos {

    private val peru: Locale = Locale.forLanguageTag("es-PE")

    fun soles(valor: Double): String = String.format(Locale.US, "S/ %.2f", valor)

    fun fecha(milisegundos: Long): String =
        if (milisegundos <= 0L) "Sin fecha"
        else SimpleDateFormat("dd/MM/yyyy HH:mm", peru).format(Date(milisegundos))

    fun distancia(km: Double): String =
        if (km < 1.0) "${(km * 1000).toInt()} m" else String.format(Locale.US, "%.1f km", km)

    fun coordenadas(lat: Double, lng: Double): String =
        String.format(Locale.US, "%.5f, %.5f", lat, lng)

    fun iniciales(nombre: String): String =
        nombre.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }
}
