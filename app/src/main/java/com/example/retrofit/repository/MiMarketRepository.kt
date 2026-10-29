package com.example.retrofit.repository

import android.net.Uri
import android.util.Log
import com.example.retrofit.data.local.PreferenciasPrivacidad
import com.example.retrofit.data.local.ProcesadorImagenes
import com.example.retrofit.data.remote.FirebaseAuthManager
import com.example.retrofit.data.remote.FirestoreRepository
import com.example.retrofit.data.remote.LocationRepository
import com.example.retrofit.data.remote.StorageRepository
import com.example.retrofit.model.Coordenada
import com.example.retrofit.model.ItemCarrito
import com.example.retrofit.model.ItemPedido
import com.example.retrofit.model.Pedido
import com.example.retrofit.model.Producto
import com.example.retrofit.model.Tienda
import com.example.retrofit.model.Usuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

sealed interface ResultadoUbicacion {
    data class Obtenida(val coordenada: Coordenada, val direccion: String?) : ResultadoUbicacion
    data object SinPermiso : ResultadoUbicacion
    data object GpsDesactivado : ResultadoUbicacion
    data class Fallo(val mensaje: String) : ResultadoUbicacion
}

data class ListaTiendas(
    val tiendas: List<Tienda>,
    val sonDeEjemplo: Boolean
)

class MiMarketRepository(
    private val auth: FirebaseAuthManager,
    private val firestore: FirestoreRepository,
    private val storage: StorageRepository,
    private val ubicacion: LocationRepository,
    private val imagenes: ProcesadorImagenes,
    private val preferencias: PreferenciasPrivacidad
) {

    val haySesion: Boolean
        get() = auth.uidActual != null

    val uidActual: String?
        get() = auth.uidActual

    val correoActual: String
        get() = auth.correoActual.orEmpty()

    var consentimientoUbicacion: Boolean
        get() = preferencias.consentimientoUbicacion
        private set(valor) {
            preferencias.consentimientoUbicacion = valor
        }

    suspend fun iniciarSesion(correo: String, clave: String): Resultado<Unit> = ejecutar {
        val uid = auth.iniciarSesion(correo, clave)
        asegurarPerfil(uid, auth.nombreActual.orEmpty(), correo)
    }

    suspend fun registrar(nombre: String, correo: String, clave: String): Resultado<Unit> = ejecutar {
        val uid = auth.registrar(nombre, correo, clave)
        val ahora = System.currentTimeMillis()
        try {
            firestore.crearPerfil(
                Usuario(
                    uid = uid,
                    nombre = nombre,
                    email = correo,
                    rol = Usuario.ROL_CLIENTE,
                    aceptoPrivacidad = true,
                    creadoEn = ahora
                )
            )
        } catch (e: Exception) {
            auth.cerrarSesion()
            throw e
        }
    }

    suspend fun asegurarPerfilActual(): Resultado<Unit> = ejecutar {
        val uid = auth.uidActual ?: throw SinSesionException()
        asegurarPerfil(uid, auth.nombreActual.orEmpty(), correoActual)
    }

    suspend fun recuperarClave(correo: String): Resultado<Unit> = ejecutar {
        auth.enviarRecuperacion(correo)
    }

    fun cerrarSesion() {
        auth.cerrarSesion()
    }

    fun productos(): Flow<Resultado<List<Producto>>> =
        firestore.escucharProductos()
            .map<List<Producto>, Resultado<List<Producto>>> { Resultado.Exito(it) }
            .catch { emit(Resultado.Fallo(traducir(it))) }

    suspend fun producto(id: String): Resultado<Producto?> = ejecutar {
        firestore.obtenerProducto(id)
    }

    suspend fun guardarProducto(producto: Producto, nuevaImagen: Uri?): Resultado<Unit> = ejecutar {
        val uid = auth.uidActual ?: throw SinSesionException()
        var aGuardar = producto
        val rutaAnterior = producto.imagenRuta
        if (nuevaImagen != null) {
            val bytes = imagenes.comprimir(nuevaImagen) ?: throw ErrorDeImagen()
            val subida = storage.subirImagenProducto(uid, bytes)
            aGuardar = aGuardar.copy(imagenUrl = subida.url, imagenRuta = subida.ruta)
        }
        val ahora = System.currentTimeMillis()
        if (aGuardar.id.isBlank()) {
            firestore.crearProducto(aGuardar.copy(creadorUid = uid, creadoEn = ahora, actualizadoEn = ahora))
        } else {
            firestore.actualizarProducto(aGuardar.copy(actualizadoEn = ahora))
        }
        if (nuevaImagen != null && rutaAnterior.isNotBlank()) {
            try {
                storage.eliminar(rutaAnterior)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo borrar la imagen anterior", e)
            }
        }
        Unit
    }

    suspend fun eliminarProducto(producto: Producto): Resultado<Unit> = ejecutar {
        firestore.eliminarProducto(producto.id)
        if (producto.imagenRuta.isNotBlank()) {
            try {
                storage.eliminar(producto.imagenRuta)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo borrar la imagen del producto", e)
            }
        }
        Unit
    }

    fun perfil(): Flow<Resultado<Usuario?>> {
        val uid = auth.uidActual ?: return flowOf(Resultado.Fallo(traducir(SinSesionException())))
        return firestore.escucharPerfil(uid)
            .map<Usuario?, Resultado<Usuario?>> { Resultado.Exito(it) }
            .catch { emit(Resultado.Fallo(traducir(it))) }
    }

    suspend fun actualizarNombre(nombre: String): Resultado<Unit> = ejecutar {
        val uid = auth.uidActual ?: throw SinSesionException()
        auth.actualizarNombre(nombre)
        firestore.actualizarCampos(uid, mapOf("nombre" to nombre))
    }

    suspend fun guardarEntrega(coordenada: Coordenada, direccion: String): Resultado<Unit> = ejecutar {
        val uid = auth.uidActual ?: throw SinSesionException()
        firestore.actualizarCampos(
            uid,
            mapOf(
                "direccionEntrega" to direccion,
                "latEntrega" to coordenada.lat,
                "lngEntrega" to coordenada.lng
            )
        )
    }

    suspend fun registrarConsentimientoUbicacion(acepta: Boolean): Resultado<Unit> {
        consentimientoUbicacion = acepta
        return ejecutar {
            val uid = auth.uidActual ?: throw SinSesionException()
            firestore.actualizarCampos(
                uid,
                mapOf(
                    "consentimientoUbicacion" to acepta,
                    "fechaConsentimientoUbicacion" to System.currentTimeMillis()
                )
            )
        }
    }

    fun pedidos(): Flow<Resultado<List<Pedido>>> {
        val uid = auth.uidActual ?: return flowOf(Resultado.Fallo(traducir(SinSesionException())))
        return firestore.escucharPedidos(uid)
            .map<List<Pedido>, Resultado<List<Pedido>>> { Resultado.Exito(it) }
            .catch { emit(Resultado.Fallo(traducir(it))) }
    }

    suspend fun confirmarPedido(
        items: List<ItemCarrito>,
        direccion: String,
        entrega: Coordenada?
    ): Resultado<String> = ejecutar {
        val uid = auth.uidActual ?: throw SinSesionException()
        val pedido = Pedido(
            items = items.map {
                ItemPedido(
                    productoId = it.producto.id,
                    nombre = it.producto.nombre,
                    precioUnitario = it.producto.precio,
                    cantidad = it.cantidad
                )
            },
            total = items.sumOf { it.subtotal },
            direccionEntrega = direccion,
            latEntrega = entrega?.lat,
            lngEntrega = entrega?.lng,
            fecha = System.currentTimeMillis()
        )
        firestore.crearPedido(uid, pedido)
    }

    suspend fun tiendas(): Resultado<ListaTiendas> = ejecutar {
        val remotas = firestore.obtenerTiendas()
        if (remotas.isEmpty()) ListaTiendas(Tienda.DE_EJEMPLO, sonDeEjemplo = true)
        else ListaTiendas(remotas, sonDeEjemplo = false)
    }

    suspend fun publicarTiendasDeEjemplo(): Resultado<Unit> = ejecutar {
        firestore.publicarTiendas(Tienda.DE_EJEMPLO)
    }

    fun tienePermisoUbicacion(): Boolean = ubicacion.tienePermiso()

    suspend fun ubicacionActual(): ResultadoUbicacion {
        if (!ubicacion.tienePermiso()) return ResultadoUbicacion.SinPermiso
        if (!ubicacion.gpsActivado()) return ResultadoUbicacion.GpsDesactivado
        return try {
            val coordenada = ubicacion.ubicacionActual()
                ?: return ResultadoUbicacion.Fallo("No se pudo obtener tu ubicación. Inténtalo de nuevo al aire libre.")
            ResultadoUbicacion.Obtenida(coordenada, ubicacion.direccionDe(coordenada))
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            ResultadoUbicacion.SinPermiso
        } catch (e: Exception) {
            Log.w(TAG, "Error de ubicación", e)
            ResultadoUbicacion.Fallo("Ocurrió un error al obtener tu ubicación, verifica tu GPS.")
        }
    }

    suspend fun direccionDe(coordenada: Coordenada): String? = ubicacion.direccionDe(coordenada)

    fun distanciaKm(desde: Coordenada, hasta: Coordenada): Double = ubicacion.distanciaKm(desde, hasta)

    suspend fun eliminarCuenta(clave: String): Resultado<Unit> = ejecutar {
        val uid = auth.uidActual ?: throw SinSesionException()
        auth.reautenticar(clave)
        firestore.eliminarDatosDeUsuario(uid)
        preferencias.limpiar()
        auth.eliminarCuenta()
    }

    private suspend fun asegurarPerfil(uid: String, nombre: String, correo: String) {
        if (firestore.obtenerPerfil(uid) == null) {
            firestore.crearPerfil(
                Usuario(
                    uid = uid,
                    nombre = nombre.ifBlank { correo.substringBefore('@') }.take(40),
                    email = correo,
                    rol = Usuario.ROL_CLIENTE,
                    aceptoPrivacidad = true,
                    creadoEn = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun <T> ejecutar(bloque: suspend () -> T): Resultado<T> =
        try {
            Resultado.Exito(bloque())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Operación fallida", e)
            Resultado.Fallo(traducir(e))
        }

    private fun traducir(error: Throwable): String = TraductorErrores.mensaje(error)

    private companion object {
        const val TAG = "MiMarket"
    }
}
