package com.example.retrofit.view.navegacion

object Rutas {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val CATALOGO = "catalogo"
    const val PRODUCTO = "producto?id={id}"
    const val CARRITO = "carrito"
    const val MAPA = "mapa?lat={lat}&lng={lng}"
    const val PERFIL = "perfil"
    const val PEDIDOS = "pedidos"
    const val PRIVACIDAD = "privacidad"

    const val ARG_ID = "id"
    const val ARG_LAT = "lat"
    const val ARG_LNG = "lng"

    fun producto(id: String? = null): String =
        if (id.isNullOrBlank()) "producto" else "producto?id=$id"

    fun mapa(lat: Double? = null, lng: Double? = null): String =
        if (lat == null || lng == null) "mapa" else "mapa?lat=$lat&lng=$lng"
}
