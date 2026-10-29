package com.example.retrofit.data.remote

import com.example.retrofit.model.ItemPedido
import com.example.retrofit.model.Pedido
import com.example.retrofit.model.Producto
import com.example.retrofit.model.Tienda
import com.example.retrofit.model.Usuario
import com.google.firebase.Firebase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val db: FirebaseFirestore = Firebase.firestore
) {

    private val productos: CollectionReference get() = db.collection(PRODUCTOS)
    private val tiendas: CollectionReference get() = db.collection(TIENDAS)
    private val usuarios: CollectionReference get() = db.collection(USUARIOS)

    private fun pedidos(uid: String): CollectionReference =
        usuarios.document(uid).collection(PEDIDOS)

    fun escucharProductos(): Flow<List<Producto>> = callbackFlow {
        val registro = productos.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val lista = snapshot?.documents.orEmpty()
                .map { it.aProducto() }
                .sortedBy { it.nombre.lowercase() }
            trySend(lista)
        }
        awaitClose { registro.remove() }
    }

    suspend fun obtenerProducto(id: String): Producto? {
        val documento = productos.document(id).get().await()
        return if (documento.exists()) documento.aProducto() else null
    }

    suspend fun crearProducto(producto: Producto): String {
        val referencia = productos.document()
        referencia.set(producto.aMapa()).await()
        return referencia.id
    }

    suspend fun actualizarProducto(producto: Producto) {
        productos.document(producto.id).set(producto.aMapa()).await()
    }

    suspend fun eliminarProducto(id: String) {
        productos.document(id).delete().await()
    }

    suspend fun obtenerTiendas(): List<Tienda> =
        tiendas.get().await().documents
            .map { it.aTienda() }
            .sortedBy { it.nombre }

    suspend fun publicarTiendas(lista: List<Tienda>) {
        val lote = db.batch()
        lista.forEach { tienda -> lote.set(tiendas.document(tienda.id), tienda.aMapa()) }
        lote.commit().await()
    }

    suspend fun obtenerPerfil(uid: String): Usuario? {
        val documento = usuarios.document(uid).get().await()
        return if (documento.exists()) documento.aUsuario() else null
    }

    fun escucharPerfil(uid: String): Flow<Usuario?> = callbackFlow {
        val registro = usuarios.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(if (snapshot != null && snapshot.exists()) snapshot.aUsuario() else null)
        }
        awaitClose { registro.remove() }
    }

    suspend fun crearPerfil(usuario: Usuario) {
        usuarios.document(usuario.uid).set(usuario.aMapa()).await()
    }

    suspend fun actualizarCampos(uid: String, campos: Map<String, Any>) {
        usuarios.document(uid).update(campos).await()
    }

    fun escucharPedidos(uid: String): Flow<List<Pedido>> = callbackFlow {
        val registro = pedidos(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val lista = snapshot?.documents.orEmpty()
                .map { it.aPedido() }
                .sortedByDescending { it.fecha }
            trySend(lista)
        }
        awaitClose { registro.remove() }
    }

    suspend fun crearPedido(uid: String, pedido: Pedido): String {
        val referencia = pedidos(uid).document()
        referencia.set(pedido.aMapa()).await()
        return referencia.id
    }

    suspend fun eliminarDatosDeUsuario(uid: String) {
        val documentos = pedidos(uid).get().await().documents
        val lote = db.batch()
        documentos.forEach { lote.delete(it.reference) }
        lote.delete(usuarios.document(uid))
        lote.commit().await()
    }

    private fun DocumentSnapshot.aProducto() = Producto(
        id = id,
        nombre = getString("nombre").orEmpty(),
        descripcion = getString("descripcion").orEmpty(),
        categoria = getString("categoria").orEmpty(),
        precio = getDouble("precio") ?: 0.0,
        stock = (getLong("stock") ?: 0L).toInt(),
        imagenUrl = getString("imagenUrl").orEmpty(),
        imagenRuta = getString("imagenRuta").orEmpty(),
        creadorUid = getString("creadorUid").orEmpty(),
        creadoEn = getLong("creadoEn") ?: 0L,
        actualizadoEn = getLong("actualizadoEn") ?: 0L
    )

    private fun Producto.aMapa(): Map<String, Any> = mapOf(
        "nombre" to nombre,
        "descripcion" to descripcion,
        "categoria" to categoria,
        "precio" to precio,
        "stock" to stock,
        "imagenUrl" to imagenUrl,
        "imagenRuta" to imagenRuta,
        "creadorUid" to creadorUid,
        "creadoEn" to creadoEn,
        "actualizadoEn" to actualizadoEn
    )

    private fun DocumentSnapshot.aTienda() = Tienda(
        id = id,
        nombre = getString("nombre").orEmpty(),
        direccion = getString("direccion").orEmpty(),
        lat = getDouble("lat") ?: 0.0,
        lng = getDouble("lng") ?: 0.0,
        horario = getString("horario").orEmpty(),
        telefono = getString("telefono").orEmpty()
    )

    private fun Tienda.aMapa(): Map<String, Any> = mapOf(
        "nombre" to nombre,
        "direccion" to direccion,
        "lat" to lat,
        "lng" to lng,
        "horario" to horario,
        "telefono" to telefono
    )

    private fun DocumentSnapshot.aUsuario() = Usuario(
        uid = id,
        nombre = getString("nombre").orEmpty(),
        email = getString("email").orEmpty(),
        rol = getString("rol") ?: Usuario.ROL_CLIENTE,
        direccionEntrega = getString("direccionEntrega").orEmpty(),
        latEntrega = getDouble("latEntrega"),
        lngEntrega = getDouble("lngEntrega"),
        aceptoPrivacidad = getBoolean("aceptoPrivacidad") ?: false,
        consentimientoUbicacion = getBoolean("consentimientoUbicacion") ?: false,
        creadoEn = getLong("creadoEn") ?: 0L
    )

    private fun Usuario.aMapa(): Map<String, Any> {
        val mapa = mutableMapOf<String, Any>(
            "nombre" to nombre,
            "email" to email,
            "rol" to rol,
            "direccionEntrega" to direccionEntrega,
            "aceptoPrivacidad" to aceptoPrivacidad,
            "consentimientoUbicacion" to consentimientoUbicacion,
            "creadoEn" to creadoEn
        )
        latEntrega?.let { mapa["latEntrega"] = it }
        lngEntrega?.let { mapa["lngEntrega"] = it }
        return mapa
    }

    private fun DocumentSnapshot.aPedido(): Pedido {
        val items = (get("items") as? List<*>).orEmpty().mapNotNull { elemento ->
            val mapa = elemento as? Map<*, *> ?: return@mapNotNull null
            ItemPedido(
                productoId = mapa["productoId"] as? String ?: "",
                nombre = mapa["nombre"] as? String ?: "",
                precioUnitario = (mapa["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                cantidad = (mapa["cantidad"] as? Number)?.toInt() ?: 0
            )
        }
        return Pedido(
            id = id,
            items = items,
            total = getDouble("total") ?: 0.0,
            direccionEntrega = getString("direccionEntrega").orEmpty(),
            latEntrega = getDouble("latEntrega"),
            lngEntrega = getDouble("lngEntrega"),
            estado = getString("estado") ?: Pedido.ESTADO_REGISTRADO,
            fecha = getLong("fecha") ?: 0L
        )
    }

    private fun Pedido.aMapa(): Map<String, Any> {
        val mapa = mutableMapOf<String, Any>(
            "items" to items.map {
                mapOf(
                    "productoId" to it.productoId,
                    "nombre" to it.nombre,
                    "precioUnitario" to it.precioUnitario,
                    "cantidad" to it.cantidad
                )
            },
            "total" to total,
            "direccionEntrega" to direccionEntrega,
            "estado" to estado,
            "fecha" to fecha
        )
        latEntrega?.let { mapa["latEntrega"] = it }
        lngEntrega?.let { mapa["lngEntrega"] = it }
        return mapa
    }

    companion object {
        const val PRODUCTOS = "productos"
        const val TIENDAS = "tiendas"
        const val USUARIOS = "usuarios"
        const val PEDIDOS = "pedidos"
    }
}
