package com.example.retrofit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.retrofit.di.Dependencias
import com.example.retrofit.model.Pedido
import com.example.retrofit.model.Usuario
import com.example.retrofit.repository.MiMarketRepository
import com.example.retrofit.repository.Resultado
import com.example.retrofit.util.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PerfilViewModel(
    private val repositorio: MiMarketRepository = Dependencias.repositorio
) : ViewModel() {

    val perfil: StateFlow<EstadoUi<Usuario?>> = repositorio.perfil()
        .map { it.aEstado() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoUi.Cargando)

    val pedidos: StateFlow<EstadoUi<List<Pedido>>> = repositorio.pedidos()
        .map { it.aEstado() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoUi.Cargando)

    private val _consentimiento = MutableStateFlow(repositorio.consentimientoUbicacion)
    val consentimiento: StateFlow<Boolean> = _consentimiento.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    private val _procesando = MutableStateFlow(false)
    val procesando: StateFlow<Boolean> = _procesando.asStateFlow()

    private val _eliminacion = MutableStateFlow<EstadoUi<Unit>>(EstadoUi.Inactivo)
    val eliminacion: StateFlow<EstadoUi<Unit>> = _eliminacion.asStateFlow()

    val correo: String
        get() = repositorio.correoActual

    fun cambiarConsentimiento(valor: Boolean) {
        _consentimiento.value = valor
        viewModelScope.launch {
            repositorio.registrarConsentimientoUbicacion(valor)
        }
    }

    fun actualizarNombre(nombre: String): String? {
        val error = Validaciones.nombrePersona(nombre)
        if (error != null) return error
        viewModelScope.launch {
            _procesando.value = true
            _mensaje.value = when (val resultado = repositorio.actualizarNombre(nombre.trim())) {
                is Resultado.Exito -> "Nombre actualizado"
                is Resultado.Fallo -> resultado.mensaje
            }
            _procesando.value = false
        }
        return null
    }

    fun cerrarSesion() {
        repositorio.cerrarSesion()
    }

    fun eliminarCuenta(clave: String) {
        if (clave.isEmpty()) {
            _eliminacion.value = EstadoUi.Error("Escribe tu contraseña para confirmar")
            return
        }
        viewModelScope.launch {
            _eliminacion.value = EstadoUi.Cargando
            _eliminacion.value = repositorio.eliminarCuenta(clave).aEstado()
        }
    }

    fun descartarEliminacion() {
        _eliminacion.value = EstadoUi.Inactivo
    }

    fun descartarMensaje() {
        _mensaje.value = null
    }
}
