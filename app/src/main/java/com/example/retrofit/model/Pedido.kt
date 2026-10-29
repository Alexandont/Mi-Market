package com.example.retrofit.model

data class ItemPedido(
    val productoId: String = "",
    val nombre: String = "",
    val precioUnitario: Double = 0.0,
    val cantidad: Int = 0
) {
    val subtotal: Double get() = precioUnitario * cantidad
}

data class Pedido(
    val id: String = "",
    val items: List<ItemPedido> = emptyList(),
    val total: Double = 0.0,
    val direccionEntrega: String = "",
    val latEntrega: Double? = null,
    val lngEntrega: Double? = null,
    val estado: String = ESTADO_REGISTRADO,
    val fecha: Long = 0L
) {
    val cantidadProductos: Int get() = items.sumOf { it.cantidad }

    companion object {
        const val ESTADO_REGISTRADO = "Registrado"
    }
}
