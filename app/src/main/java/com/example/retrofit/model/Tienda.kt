package com.example.retrofit.model

data class Tienda(
    val id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val horario: String = "",
    val telefono: String = ""
) {
    val coordenada: Coordenada get() = Coordenada(lat, lng)

    companion object {
        val DE_EJEMPLO = listOf(
            Tienda("miraflores", "Mi Market Miraflores", "Av. José Larco 345, Miraflores", -12.1219, -77.0297, "Lun a Dom 7:00 a 23:00", "01 445 1200"),
            Tienda("san-isidro", "Mi Market San Isidro", "Av. Javier Prado Este 1060, San Isidro", -12.0918, -77.0215, "Lun a Dom 7:00 a 22:00", "01 422 3300"),
            Tienda("surco", "Mi Market Surco", "Av. Primavera 1050, Santiago de Surco", -12.1105, -76.9890, "Lun a Dom 8:00 a 22:00", "01 372 5400"),
            Tienda("lince", "Mi Market Lince", "Av. Arequipa 2450, Lince", -12.0853, -77.0346, "Lun a Sáb 7:00 a 22:00", "01 471 6600"),
            Tienda("cercado", "Mi Market Cercado", "Jr. de la Unión 600, Cercado de Lima", -12.0489, -77.0334, "Lun a Dom 8:00 a 21:00", "01 428 7700")
        )
    }
}
