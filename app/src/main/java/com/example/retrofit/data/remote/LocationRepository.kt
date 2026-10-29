package com.example.retrofit.data.remote

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.retrofit.model.Coordenada
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LocationRepository(
    private val contexto: Context
) {

    private val cliente by lazy { LocationServices.getFusedLocationProviderClient(contexto) }

    fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun gpsActivado(): Boolean {
        val gestor = contexto.getSystemService(LocationManager::class.java) ?: return false
        return LocationManagerCompat.isLocationEnabled(gestor)
    }

    @SuppressLint("MissingPermission")
    suspend fun ubicacionActual(): Coordenada? {
        if (!tienePermiso()) throw SecurityException("Sin permiso de ubicación")
        val cancelacion = CancellationTokenSource()
        return try {
            val actual = cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelacion.token).await()
            val ubicacion = actual ?: cliente.lastLocation.await()
            ubicacion?.let { Coordenada(it.latitude, it.longitude) }
        } finally {
            cancelacion.cancel()
        }
    }

    @Suppress("DEPRECATION")
    suspend fun direccionDe(coordenada: Coordenada): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        try {
            val direccion = Geocoder(contexto, Locale.forLanguageTag("es-PE"))
                .getFromLocation(coordenada.lat, coordenada.lng, 1)
                .orEmpty()
                .firstOrNull()
                ?: return@withContext null
            val lineas = (0..direccion.maxAddressLineIndex).mapNotNull { direccion.getAddressLine(it) }
            lineas.joinToString(", ").ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    fun distanciaKm(desde: Coordenada, hasta: Coordenada): Double {
        val resultado = FloatArray(1)
        Location.distanceBetween(desde.lat, desde.lng, hasta.lat, hasta.lng, resultado)
        return resultado[0] / 1000.0
    }
}
