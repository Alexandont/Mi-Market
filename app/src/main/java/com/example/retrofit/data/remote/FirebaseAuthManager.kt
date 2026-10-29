package com.example.retrofit.data.remote

import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager(
    private val auth: FirebaseAuth = Firebase.auth
) {

    val uidActual: String?
        get() = auth.currentUser?.uid

    val correoActual: String?
        get() = auth.currentUser?.email

    val nombreActual: String?
        get() = auth.currentUser?.displayName

    suspend fun iniciarSesion(correo: String, clave: String): String {
        val resultado = auth.signInWithEmailAndPassword(correo, clave).await()
        return resultado.user?.uid ?: throw IllegalStateException("Sin usuario")
    }

    suspend fun registrar(nombre: String, correo: String, clave: String): String {
        val resultado = auth.createUserWithEmailAndPassword(correo, clave).await()
        val usuario = resultado.user ?: throw IllegalStateException("Sin usuario")
        val perfil = UserProfileChangeRequest.Builder().setDisplayName(nombre).build()
        usuario.updateProfile(perfil).await()
        return usuario.uid
    }

    suspend fun actualizarNombre(nombre: String) {
        val usuario = auth.currentUser ?: throw IllegalStateException("Sin sesión")
        val perfil = UserProfileChangeRequest.Builder().setDisplayName(nombre).build()
        usuario.updateProfile(perfil).await()
    }

    suspend fun enviarRecuperacion(correo: String) {
        auth.sendPasswordResetEmail(correo).await()
    }

    suspend fun reautenticar(clave: String) {
        val usuario = auth.currentUser ?: throw IllegalStateException("Sin sesión")
        val credencial = EmailAuthProvider.getCredential(usuario.email.orEmpty(), clave)
        usuario.reauthenticate(credencial).await()
    }

    suspend fun eliminarCuenta() {
        val usuario = auth.currentUser ?: throw IllegalStateException("Sin sesión")
        usuario.delete().await()
    }

    fun cerrarSesion() {
        auth.signOut()
    }
}
