package com.example.retrofit.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.retrofit.di.Dependencias
import com.example.retrofit.model.Producto
import com.example.retrofit.repository.MiMarketRepository
import com.example.retrofit.repository.Resultado
import com.example.retrofit.util.Validaciones
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FormularioProducto(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val categoria: String = "",
    val precio: String = "",
    val stock: String = "",
    val imagenUrl: String = "",
    val imagenRuta: String = "",
    val creadorUid: String = "",
    val creadoEn: Long = 0L,
    val imagenNueva: Uri? = null
) {
    val esEdicion: Boolean get() = id.isNotBlank()
}

data class ErroresProducto(
    val nombre: String? = null,
    val categoria: String? = null,
    val precio: String? = null,
    val stock: String? = null
) {
    val hayErrores: Boolean get() = listOf(nombre, categoria, precio, stock).any { it != null }
}

class ProductoFormViewModel(
    private val repositorio: MiMarketRepository = Dependencias.repositorio
) : ViewModel() {

    private val _formulario = MutableStateFlow(FormularioProducto())
    val formulario: StateFlow<FormularioProducto> = _formulario.asStateFlow()

    private val _errores = MutableStateFlow(ErroresProducto())
    val errores: StateFlow<ErroresProducto> = _errores.asStateFlow()

    private val _carga = MutableStateFlow<EstadoUi<Unit>>(EstadoUi.Inactivo)
    val carga: StateFlow<EstadoUi<Unit>> = _carga.asStateFlow()

    private val _guardado = MutableStateFlow<EstadoUi<Unit>>(EstadoUi.Inactivo)
    val guardado: StateFlow<EstadoUi<Unit>> = _guardado.asStateFlow()

    private val _eliminado = MutableStateFlow<EstadoUi<Unit>>(EstadoUi.Inactivo)
    val eliminado: StateFlow<EstadoUi<Unit>> = _eliminado.asStateFlow()

    private var iniciado = false

    fun cargar(id: String?) {
        if (iniciado) return
        iniciado = true
        if (id.isNullOrBlank()) return
        viewModelScope.launch {
            _carga.value = EstadoUi.Cargando
            when (val resultado = repositorio.producto(id)) {
                is Resultado.Exito -> {
                    val producto = resultado.datos
                    if (producto == null) {
                        _carga.value = EstadoUi.Error("Este producto ya no existe.")
                    } else {
                        _formulario.value = FormularioProducto(
                            id = producto.id,
                            nombre = producto.nombre,
                            descripcion = producto.descripcion,
                            categoria = producto.categoria,
                            precio = String.format(Locale.US, "%.2f", producto.precio),
                            stock = producto.stock.toString(),
                            imagenUrl = producto.imagenUrl,
                            imagenRuta = producto.imagenRuta,
                            creadorUid = producto.creadorUid,
                            creadoEn = producto.creadoEn
                        )
                        _carga.value = EstadoUi.Exito(Unit)
                    }
                }
                is Resultado.Fallo -> {
                    _carga.value = EstadoUi.Error(resultado.mensaje)
                }
            }
        }
    }

    fun actualizar(cambio: (FormularioProducto) -> FormularioProducto) {
        _formulario.value = cambio(_formulario.value)
    }

    fun limpiarError(campo: String) {
        val actual = _errores.value
        _errores.value = when (campo) {
            CAMPO_NOMBRE -> actual.copy(nombre = null)
            CAMPO_CATEGORIA -> actual.copy(categoria = null)
            CAMPO_PRECIO -> actual.copy(precio = null)
            else -> actual.copy(stock = null)
        }
    }

    fun guardar() {
        val datos = _formulario.value
        val errores = ErroresProducto(
            nombre = Validaciones.nombreProducto(datos.nombre),
            categoria = if (datos.categoria.isBlank()) "Elige una categoría" else null,
            precio = Validaciones.precio(datos.precio),
            stock = Validaciones.stock(datos.stock)
        )
        _errores.value = errores
        if (errores.hayErrores) return
        val producto = Producto(
            id = datos.id,
            nombre = datos.nombre.trim(),
            descripcion = datos.descripcion.trim(),
            categoria = datos.categoria,
            precio = datos.precio.replace(',', '.').toDouble(),
            stock = datos.stock.toInt(),
            imagenUrl = datos.imagenUrl,
            imagenRuta = datos.imagenRuta,
            creadorUid = datos.creadorUid,
            creadoEn = datos.creadoEn
        )
        viewModelScope.launch {
            _guardado.value = EstadoUi.Cargando
            _guardado.value = repositorio.guardarProducto(producto, datos.imagenNueva).aEstado()
        }
    }

    fun eliminar() {
        val datos = _formulario.value
        if (!datos.esEdicion) return
        val producto = Producto(id = datos.id, imagenRuta = datos.imagenRuta)
        viewModelScope.launch {
            _eliminado.value = EstadoUi.Cargando
            _eliminado.value = repositorio.eliminarProducto(producto).aEstado()
        }
    }

    fun descartarErrores() {
        if (_guardado.value is EstadoUi.Error) _guardado.value = EstadoUi.Inactivo
        if (_eliminado.value is EstadoUi.Error) _eliminado.value = EstadoUi.Inactivo
    }

    companion object {
        const val CAMPO_NOMBRE = "nombre"
        const val CAMPO_CATEGORIA = "categoria"
        const val CAMPO_PRECIO = "precio"
        const val CAMPO_STOCK = "stock"
    }
}
