package com.example.retrofit.util

import android.util.Patterns

object Validaciones {

    private val formatoCorreo = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    fun correo(valor: String): String? = when {
        valor.isBlank() -> "Escribe tu correo"
        !Patterns.EMAIL_ADDRESS.matcher(valor).matches() || !formatoCorreo.matches(valor) ->
            "Escribe un correo válido, por ejemplo nombre@gmail.com"
        else -> null
    }

    fun claveIngreso(valor: String): String? =
        if (valor.isEmpty()) "Escribe tu contraseña" else null

    fun claveNueva(valor: String): String? = when {
        valor.length < 8 -> "Usa al menos 8 caracteres"
        valor.none { it.isLetter() } || valor.none { it.isDigit() } -> "Combina letras y números"
        else -> null
    }

    fun confirmacion(clave: String, confirmacion: String): String? = when {
        confirmacion.isEmpty() -> "Repite la contraseña"
        confirmacion != clave -> "Las contraseñas no coinciden"
        else -> null
    }

    fun nombrePersona(valor: String): String? =
        if (valor.trim().length !in 2..40) "Escribe tu nombre (2 a 40 caracteres)" else null

    fun nombreProducto(valor: String): String? =
        if (valor.trim().length !in 2..80) "Escribe el nombre (2 a 80 caracteres)" else null

    fun precio(valor: String): String? {
        val numero = valor.replace(',', '.').toDoubleOrNull()
        return when {
            numero == null -> "Escribe un precio válido"
            numero <= 0.0 -> "El precio debe ser mayor a 0"
            numero > 100000.0 -> "El precio es demasiado alto"
            else -> null
        }
    }

    fun stock(valor: String): String? {
        val numero = valor.toIntOrNull()
        return when {
            numero == null -> "Escribe una cantidad entera"
            numero < 0 -> "El stock no puede ser negativo"
            else -> null
        }
    }
}
