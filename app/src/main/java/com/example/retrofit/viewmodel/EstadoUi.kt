package com.example.retrofit.viewmodel

import com.example.retrofit.model.Coordenada
import com.example.retrofit.repository.Resultado

sealed interface EstadoUi<out T> {
    data object Inactivo : EstadoUi<Nothing>
    data object Cargando : EstadoUi<Nothing>
    data class Exito<out T>(val datos: T) : EstadoUi<T>
    data class Error(val mensaje: String) : EstadoUi<Nothing>
}

sealed interface EstadoUbicacion {
    data object SinSolicitar : EstadoUbicacion
    data object Cargando : EstadoUbicacion
    data class Obtenida(val coordenada: Coordenada, val direccion: String?) : EstadoUbicacion
    data object PermisoDenegado : EstadoUbicacion
    data object GpsDesactivado : EstadoUbicacion
    data class Error(val mensaje: String) : EstadoUbicacion
}

fun <T> Resultado<T>.aEstado(): EstadoUi<T> = when (this) {
    is Resultado.Exito -> EstadoUi.Exito(datos)
    is Resultado.Fallo -> EstadoUi.Error(mensaje)
}
