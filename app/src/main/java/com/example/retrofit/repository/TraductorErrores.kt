package com.example.retrofit.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import java.io.IOException

class ErrorDeImagen : Exception("No se pudo leer la imagen")

class SinSesionException : Exception("No hay sesión activa")

object TraductorErrores {

    fun mensaje(error: Throwable?): String = when (error) {
        is FirebaseNetworkException -> SIN_CONEXION
        is FirebaseTooManyRequestsException -> "Demasiados intentos. Espera unos minutos e inténtalo de nuevo."
        is FirebaseAuthException -> deAuth(error.errorCode)
        is FirebaseFirestoreException -> deFirestore(error.code)
        is StorageException -> deStorage(error.errorCode)
        is ErrorDeImagen -> "No se pudo leer la imagen seleccionada. Prueba con otra foto."
        is SinSesionException -> "Tu sesión terminó. Vuelve a iniciar sesión."
        is SecurityException -> "La app no tiene permiso de ubicación."
        is IOException -> SIN_CONEXION
        else -> "Ocurrió un error inesperado. Inténtalo de nuevo."
    }

    private fun deAuth(codigo: String): String = when (codigo) {
        "ERROR_INVALID_EMAIL" -> "El correo no tiene un formato válido."
        "ERROR_WRONG_PASSWORD",
        "ERROR_INVALID_CREDENTIAL",
        "ERROR_USER_NOT_FOUND" -> "Correo o contraseña incorrectos."
        "ERROR_USER_DISABLED" -> "Esta cuenta fue deshabilitada. Comunícate con Mi Market."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Ese correo ya tiene una cuenta. Inicia sesión."
        "ERROR_WEAK_PASSWORD" -> "La contraseña es muy débil. Usa al menos 8 caracteres con letras y números."
        "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Espera unos minutos e inténtalo de nuevo."
        "ERROR_REQUIRES_RECENT_LOGIN" -> "Por seguridad, vuelve a iniciar sesión e inténtalo otra vez."
        "ERROR_OPERATION_NOT_ALLOWED" -> "El inicio de sesión con correo no está habilitado en Firebase."
        else -> "No se pudo completar la autenticación. Inténtalo de nuevo."
    }

    private fun deFirestore(codigo: FirebaseFirestoreException.Code): String = when (codigo) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> "No tienes permiso para realizar esta acción."
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> "Tu sesión terminó. Vuelve a iniciar sesión."
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> SIN_CONEXION
        FirebaseFirestoreException.Code.NOT_FOUND -> "La información solicitada ya no existe."
        else -> "Ocurrió un error al cargar los datos, verifica tu conexión."
    }

    private fun deStorage(codigo: Int): String = when (codigo) {
        StorageException.ERROR_NOT_AUTHORIZED -> "No tienes permiso para subir esta imagen."
        StorageException.ERROR_NOT_AUTHENTICATED -> "Tu sesión terminó. Vuelve a iniciar sesión."
        StorageException.ERROR_BUCKET_NOT_FOUND,
        StorageException.ERROR_OBJECT_NOT_FOUND -> "El almacenamiento de imágenes no está disponible. Revisa la configuración de Storage."
        StorageException.ERROR_QUOTA_EXCEEDED -> "Se superó la cuota de almacenamiento."
        StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> SIN_CONEXION
        StorageException.ERROR_CANCELED -> "La subida de la imagen se canceló."
        else -> "No se pudo subir la imagen. Inténtalo de nuevo."
    }

    private const val SIN_CONEXION = "Sin conexión a internet. Verifica tu red e inténtalo de nuevo."
}
