package com.example.retrofit.model

data class Producto(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val categoria: String = "",
    val precio: Double = 0.0,
    val stock: Int = 0,
    val imagenUrl: String = "",
    val imagenRuta: String = "",
    val creadorUid: String = "",
    val creadoEn: Long = 0L,
    val actualizadoEn: Long = 0L
) {
    companion object {
        val CATEGORIAS = listOf(
            "Abarrotes",
            "Bebidas",
            "Lácteos",
            "Frutas y verduras",
            "Carnes",
            "Panadería",
            "Limpieza",
            "Snacks",
            "Otros"
        )
    }
}
