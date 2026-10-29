package com.example.retrofit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.retrofit.di.Dependencias
import com.example.retrofit.model.Coordenada
import com.example.retrofit.model.ItemCarrito
import com.example.retrofit.model.Producto
import com.example.retrofit.model.Usuario
import com.example.retrofit.repository.MiMarketRepository
import com.example.retrofit.repository.Resultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CatalogoViewModel(
    private val repositorio: MiMarketRepository = Dependencias.repositorio
) : ViewModel() {

    private val productos: StateFlow<EstadoUi<List<Producto>>> = repositorio.productos()
        .map { it.aEstado() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoUi.Cargando)

    val perfil: StateFlow<Usuario?> = repositorio.perfil()
        .map { resultado ->
            when (resultado) {
                is Resultado.Exito -> resultado.datos
                is Resultado.Fallo -> null
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    private val _categoria = MutableStateFlow<String?>(null)
    val categoria: StateFlow<String?> = _categoria.asStateFlow()

    val catalogo: StateFlow<EstadoUi<List<Producto>>> =
        combine(productos, _busqueda, _categoria) { estado, texto, seleccion ->
            if (estado is EstadoUi.Exito) {
                val consulta = texto.trim()
                EstadoUi.Exito(
                    estado.datos.filter { producto ->
                        (seleccion == null || producto.categoria == seleccion) &&
                            (consulta.isEmpty() ||
                                producto.nombre.contains(consulta, ignoreCase = true) ||
                                producto.descripcion.contains(consulta, ignoreCase = true))
                    }
                )
            } else {
                estado
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoUi.Cargando)

    private val _carrito = MutableStateFlow<Map<String, Int>>(emptyMap())
    val carrito: StateFlow<Map<String, Int>> = _carrito.asStateFlow()

    val itemsCarrito: StateFlow<List<ItemCarrito>> =
        combine(productos, _carrito) { estado, mapa ->
            val lista = when (estado) {
                is EstadoUi.Exito -> estado.datos
                else -> emptyList()
            }
            mapa.mapNotNull { (id, cantidad) ->
                lista.firstOrNull { it.id == id }?.let { ItemCarrito(it, cantidad) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _pedido = MutableStateFlow<EstadoUi<String>>(EstadoUi.Inactivo)
    val pedido: StateFlow<EstadoUi<String>> = _pedido.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    val uidActual: String?
        get() = repositorio.uidActual

    init {
        viewModelScope.launch {
            repositorio.asegurarPerfilActual()
        }
    }

    fun puedeEditar(producto: Producto): Boolean =
        producto.creadorUid == repositorio.uidActual || perfil.value?.esAdmin == true

    fun buscar(texto: String) {
        _busqueda.value = texto
    }

    fun elegirCategoria(categoria: String?) {
        _categoria.value = if (_categoria.value == categoria) null else categoria
    }

    fun agregar(producto: Producto) {
        val actual = _carrito.value[producto.id] ?: 0
        if (actual >= producto.stock) {
            _mensaje.value = if (producto.stock <= 0) "${producto.nombre} está agotado"
            else "Solo hay ${producto.stock} unidades de ${producto.nombre}"
            return
        }
        _carrito.value = _carrito.value + (producto.id to actual + 1)
    }

    fun quitar(producto: Producto) {
        val actual = _carrito.value[producto.id] ?: return
        _carrito.value = if (actual <= 1) _carrito.value - producto.id
        else _carrito.value + (producto.id to actual - 1)
    }

    fun eliminarDelCarrito(producto: Producto) {
        _carrito.value = _carrito.value - producto.id
    }

    fun confirmarPedido() {
        val items = itemsCarrito.value
        if (items.isEmpty()) {
            _mensaje.value = "Tu carrito está vacío"
            return
        }
        val usuario = perfil.value
        val lat = usuario?.latEntrega
        val lng = usuario?.lngEntrega
        if (usuario == null || lat == null || lng == null) {
            _mensaje.value = "Elige tu dirección de entrega en el mapa antes de confirmar"
            return
        }
        viewModelScope.launch {
            _pedido.value = EstadoUi.Cargando
            val resultado = repositorio.confirmarPedido(items, usuario.direccionEntrega, Coordenada(lat, lng))
            _pedido.value = resultado.aEstado()
            if (resultado is Resultado.Exito) {
                _carrito.value = emptyMap()
            }
        }
    }

    fun descartarPedido() {
        _pedido.value = EstadoUi.Inactivo
    }

    fun descartarMensaje() {
        _mensaje.value = null
    }
}
