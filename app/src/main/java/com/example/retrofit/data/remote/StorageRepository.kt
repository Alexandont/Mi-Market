package com.example.retrofit.data.remote

import com.google.firebase.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import java.util.UUID
import kotlinx.coroutines.tasks.await

class StorageRepository(
    private val storage: FirebaseStorage = Firebase.storage
) {

    data class ImagenSubida(val url: String, val ruta: String)

    suspend fun subirImagenProducto(uid: String, bytes: ByteArray): ImagenSubida {
        val ruta = "productos/$uid/${UUID.randomUUID()}.jpg"
        val referencia = storage.reference.child(ruta)
        val metadatos = StorageMetadata.Builder().setContentType("image/jpeg").build()
        referencia.putBytes(bytes, metadatos).await()
        val url = referencia.downloadUrl.await().toString()
        return ImagenSubida(url, ruta)
    }

    suspend fun eliminar(ruta: String) {
        if (ruta.isBlank()) return
        storage.reference.child(ruta).delete().await()
    }
}
