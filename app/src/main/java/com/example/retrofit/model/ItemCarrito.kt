package com.example.retrofit.model

data class ItemCarrito(
    val producto: Producto,
    val cantidad: Int
) {
    val subtotal: Double get() = producto.precio * cantidad
}
