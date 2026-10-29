package com.example.retrofit.data.local

import android.content.Context

class PreferenciasPrivacidad(contexto: Context) {

    private val preferencias = contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    var consentimientoUbicacion: Boolean
        get() = preferencias.getBoolean(CLAVE_UBICACION, false)
        set(valor) {
            preferencias.edit().putBoolean(CLAVE_UBICACION, valor).apply()
        }

    fun limpiar() {
        preferencias.edit().clear().apply()
    }

    private companion object {
        const val ARCHIVO = "privacidad_mi_market"
        const val CLAVE_UBICACION = "consentimiento_ubicacion"
    }
}
