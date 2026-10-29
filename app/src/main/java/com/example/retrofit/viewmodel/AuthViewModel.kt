package com.example.retrofit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.retrofit.di.Dependencias
import com.example.retrofit.repository.MiMarketRepository
import com.example.retrofit.repository.Resultado
import com.example.retrofit.util.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ErroresFormulario(
    val nombre: String? = null,
    val correo: String? = null,
    val clave: String? = null,
    val confirmacion: String? = null,
    val privacidad: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(nombre, correo, clave, confirmacion, privacidad).any { it != null }
}

class AuthViewModel(
    private val repositorio: MiMarketRepository = Dependencias.repositorio
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoUi<Unit>>(EstadoUi.Inactivo)
    val estado: StateFlow<EstadoUi<Unit>> = _estado.asStateFlow()

    private val _errores = MutableStateFlow(ErroresFormulario())
    val errores: StateFlow<ErroresFormulario> = _errores.asStateFlow()

    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    val haySesion: Boolean
        get() = repositorio.haySesion

    fun iniciarSesion(correo: String, clave: String) {
        val errores = ErroresFormulario(
            correo = Validaciones.correo(correo.trim()),
            clave = Validaciones.claveIngreso(clave)
        )
        _errores.value = errores
        if (errores.hayErrores) return
        ejecutar { repositorio.iniciarSesion(correo.trim(), clave) }
    }

    fun registrar(nombre: String, correo: String, clave: String, confirmacion: String, aceptaPrivacidad: Boolean) {
        val errores = ErroresFormulario(
            nombre = Validaciones.nombrePersona(nombre),
            correo = Validaciones.correo(correo.trim()),
            clave = Validaciones.claveNueva(clave),
            confirmacion = Validaciones.confirmacion(clave, confirmacion),
            privacidad = if (aceptaPrivacidad) null else "Debes aceptar la política de privacidad"
        )
        _errores.value = errores
        if (errores.hayErrores) return
        ejecutar { repositorio.registrar(nombre.trim(), correo.trim(), clave) }
    }

    fun recuperarClave(correo: String) {
        val error = Validaciones.correo(correo.trim())
        _errores.value = ErroresFormulario(correo = error)
        if (error != null) return
        viewModelScope.launch {
            _estado.value = EstadoUi.Cargando
            when (val resultado = repositorio.recuperarClave(correo.trim())) {
                is Resultado.Exito -> {
                    _estado.value = EstadoUi.Inactivo
                    _aviso.value = "Si ${correo.trim()} tiene una cuenta, te enviamos un enlace para crear una nueva contraseña."
                }
                is Resultado.Fallo -> {
                    _estado.value = EstadoUi.Error(resultado.mensaje)
                }
            }
        }
    }

    fun limpiarCampo(campo: String) {
        val actual = _errores.value
        _errores.value = when (campo) {
            CAMPO_NOMBRE -> actual.copy(nombre = null)
            CAMPO_CORREO -> actual.copy(correo = null)
            CAMPO_CLAVE -> actual.copy(clave = null)
            CAMPO_CONFIRMACION -> actual.copy(confirmacion = null)
            else -> actual.copy(privacidad = null)
        }
    }

    fun descartarMensajes() {
        _aviso.value = null
        if (_estado.value is EstadoUi.Error) _estado.value = EstadoUi.Inactivo
    }

    private fun ejecutar(accion: suspend () -> Resultado<Unit>) {
        viewModelScope.launch {
            _estado.value = EstadoUi.Cargando
            _estado.value = accion().aEstado()
        }
    }

    companion object {
        const val CAMPO_NOMBRE = "nombre"
        const val CAMPO_CORREO = "correo"
        const val CAMPO_CLAVE = "clave"
        const val CAMPO_CONFIRMACION = "confirmacion"
        const val CAMPO_PRIVACIDAD = "privacidad"
    }
}
