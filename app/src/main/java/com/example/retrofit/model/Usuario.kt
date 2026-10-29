package com.example.retrofit.model

data class Usuario(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val rol: String = ROL_CLIENTE,
    val direccionEntrega: String = "",
    val latEntrega: Double? = null,
    val lngEntrega: Double? = null,
    val aceptoPrivacidad: Boolean = false,
    val consentimientoUbicacion: Boolean = false,
    val creadoEn: Long = 0L
) {
    val esAdmin: Boolean get() = rol == ROL_ADMIN

    val tieneEntrega: Boolean get() = latEntrega != null && lngEntrega != null

    companion object {
        const val ROL_CLIENTE = "cliente"
        const val ROL_ADMIN = "admin"
    }
}
