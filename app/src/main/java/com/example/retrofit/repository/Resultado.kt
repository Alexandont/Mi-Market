package com.example.retrofit.repository

sealed interface Resultado<out T> {
    data class Exito<out T>(val datos: T) : Resultado<T>
    data class Fallo(val mensaje: String) : Resultado<Nothing>
}
