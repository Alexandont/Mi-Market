package com.example.retrofit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.retrofit.di.Dependencias
import com.example.retrofit.model.Coordenada
import com.example.retrofit.model.Tienda
import com.example.retrofit.model.Usuario
import com.example.retrofit.repository.ListaTiendas
import com.example.retrofit.repository.MiMarketRepository
import com.example.retrofit.repository.Resultado
import com.example.retrofit.repository.ResultadoUbicacion
import com.example.retrofit.util.Formatos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TiendaCercana(
    val tienda: Tienda,
    val distanciaKm: Double?
)

class TiendasViewModel(
    private val repositorio: MiMarketRepository = Dependencias.repositorio
) : ViewModel() {

    private val _tiendas = MutableStateFlow<EstadoUi<ListaTiendas>>(EstadoUi.Cargando)
    val tiendas: StateFlow<EstadoUi<ListaTiendas>> = _tiendas.asStateFlow()

    private val _ubicacion = MutableStateFlow<EstadoUbicacion>(EstadoUbicacion.SinSolicitar)
    val ubicacion: StateFlow<EstadoUbicacion> = _ubicacion.asStateFlow()

    private val _entrega = MutableStateFlow<Coordenada?>(null)
    val entrega: StateFlow<Coordenada?> = _entrega.asStateFlow()

    private val _direccionEntrega = MutableStateFlow<String?>(null)
    val direccionEntrega: StateFlow<String?> = _direccionEntrega.asStateFlow()

    private val _guardandoEntrega = MutableStateFlow(false)
    val guardandoEntrega: StateFlow<Boolean> = _guardandoEntrega.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    val perfil: StateFlow<Usuario?> = repositorio.perfil()
        .map { resultado ->
            when (resultado) {
                is Resultado.Exito -> resultado.datos
                is Resultado.Fallo -> null
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val tiendasCercanas: StateFlow<List<TiendaCercana>> =
        combine(_tiendas, _ubicacion) { estadoTiendas, estadoUbicacion ->
            val lista = when (estadoTiendas) {
                is EstadoUi.Exito -> estadoTiendas.datos.tiendas
                else -> emptyList()
            }
            val origen = (estadoUbicacion as? EstadoUbicacion.Obtenida)?.coordenada
            lista.map { tienda ->
                TiendaCercana(tienda, origen?.let { repositorio.distanciaKm(it, tienda.coordenada) })
            }.sortedBy { it.distanciaKm ?: Double.MAX_VALUE }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val necesitaConsentimiento: Boolean
        get() = !repositorio.consentimientoUbicacion

    init {
        cargarTiendas()
        if (repositorio.consentimientoUbicacion && repositorio.tienePermisoUbicacion()) {
            obtenerUbicacion()
        }
    }

    fun cargarTiendas() {
        viewModelScope.launch {
            _tiendas.value = EstadoUi.Cargando
            _tiendas.value = repositorio.tiendas().aEstado()
        }
    }

    fun aceptarConsentimiento() {
        viewModelScope.launch {
            repositorio.registrarConsentimientoUbicacion(true)
        }
    }

    fun rechazarConsentimiento() {
        _ubicacion.value = EstadoUbicacion.PermisoDenegado
        viewModelScope.launch {
            repositorio.registrarConsentimientoUbicacion(false)
        }
    }

    fun permisoDenegado() {
        _ubicacion.value = EstadoUbicacion.PermisoDenegado
    }

    fun obtenerUbicacion() {
        viewModelScope.launch {
            _ubicacion.value = EstadoUbicacion.Cargando
            _ubicacion.value = when (val resultado = repositorio.ubicacionActual()) {
                is ResultadoUbicacion.Obtenida -> EstadoUbicacion.Obtenida(resultado.coordenada, resultado.direccion)
                is ResultadoUbicacion.SinPermiso -> EstadoUbicacion.PermisoDenegado
                is ResultadoUbicacion.GpsDesactivado -> EstadoUbicacion.GpsDesactivado
                is ResultadoUbicacion.Fallo -> EstadoUbicacion.Error(resultado.mensaje)
            }
        }
    }

    fun seleccionarEntrega(coordenada: Coordenada) {
        _entrega.value = coordenada
        _direccionEntrega.value = null
        viewModelScope.launch {
            _direccionEntrega.value = repositorio.direccionDe(coordenada)
                ?: Formatos.coordenadas(coordenada.lat, coordenada.lng)
        }
    }

    fun usarMiUbicacionComoEntrega() {
        val actual = _ubicacion.value as? EstadoUbicacion.Obtenida ?: return
        _entrega.value = actual.coordenada
        _direccionEntrega.value = actual.direccion ?: Formatos.coordenadas(actual.coordenada.lat, actual.coordenada.lng)
    }

    fun guardarEntrega() {
        val coordenada = _entrega.value ?: return
        val direccion = _direccionEntrega.value ?: Formatos.coordenadas(coordenada.lat, coordenada.lng)
        viewModelScope.launch {
            _guardandoEntrega.value = true
            val resultado = repositorio.guardarEntrega(coordenada, direccion)
            _mensaje.value = when (resultado) {
                is Resultado.Exito -> "Dirección de entrega guardada"
                is Resultado.Fallo -> resultado.mensaje
            }
            _guardandoEntrega.value = false
        }
    }

    fun publicarTiendasDeEjemplo() {
        viewModelScope.launch {
            when (val resultado = repositorio.publicarTiendasDeEjemplo()) {
                is Resultado.Exito -> {
                    _mensaje.value = "Tiendas publicadas en Firestore"
                    cargarTiendas()
                }
                is Resultado.Fallo -> {
                    _mensaje.value = resultado.mensaje
                }
            }
        }
    }

    fun descartarMensaje() {
        _mensaje.value = null
    }
}
