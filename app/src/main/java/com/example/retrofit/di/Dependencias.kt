package com.example.retrofit.di

import android.content.Context
import com.example.retrofit.data.local.PreferenciasPrivacidad
import com.example.retrofit.data.local.ProcesadorImagenes
import com.example.retrofit.data.remote.FirebaseAuthManager
import com.example.retrofit.data.remote.FirestoreRepository
import com.example.retrofit.data.remote.LocationRepository
import com.example.retrofit.data.remote.StorageRepository
import com.example.retrofit.repository.MiMarketRepository

object Dependencias {

    lateinit var repositorio: MiMarketRepository
        private set

    fun iniciar(contexto: Context) {
        if (::repositorio.isInitialized) return
        val app = contexto.applicationContext
        repositorio = MiMarketRepository(
            auth = FirebaseAuthManager(),
            firestore = FirestoreRepository(),
            storage = StorageRepository(),
            ubicacion = LocationRepository(app),
            imagenes = ProcesadorImagenes(app),
            preferencias = PreferenciasPrivacidad(app)
        )
    }
}
