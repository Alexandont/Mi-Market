package com.example.retrofit

import android.app.Application
import com.example.retrofit.di.Dependencias

class MiMarketApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Dependencias.iniciar(this)
    }
}
