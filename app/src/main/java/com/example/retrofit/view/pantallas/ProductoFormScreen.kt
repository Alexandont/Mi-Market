package com.example.retrofit.view.pantallas

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retrofit.model.Producto
import com.example.retrofit.util.Formatos
import com.example.retrofit.view.componentes.BarraSuperior
import com.example.retrofit.view.componentes.BotonPrincipal
import com.example.retrofit.view.componentes.CampoTexto
import com.example.retrofit.view.componentes.DialogoConfirmacion
import com.example.retrofit.view.componentes.EstadoCargando
import com.example.retrofit.view.componentes.EstadoMensaje
import com.example.retrofit.view.componentes.ImagenProducto
import com.example.retrofit.view.componentes.TarjetaSeccion
import com.example.retrofit.viewmodel.EstadoUi
import com.example.retrofit.viewmodel.ProductoFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoFormScreen(
    productoId: String?,
    alVolver: () -> Unit,
    vm: ProductoFormViewModel = viewModel()
) {
    val formulario by vm.formulario.collectAsStateWithLifecycle()
    val errores by vm.errores.collectAsStateWithLifecycle()
    val carga by vm.carga.collectAsStateWithLifecycle()
    val guardado by vm.guardado.collectAsStateWithLifecycle()
    val eliminado by vm.eliminado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    var confirmarEliminar by rememberSaveable { mutableStateOf(false) }
    val ocupado = guardado is EstadoUi.Cargando || eliminado is EstadoUi.Cargando

    val selectorImagen = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.actualizar { it.copy(imagenNueva = uri) }
    }

    LaunchedEffect(productoId) { vm.cargar(productoId) }

    LaunchedEffect(guardado, eliminado) {
        if (guardado is EstadoUi.Exito || eliminado is EstadoUi.Exito) alVolver()
    }

    val errorOperacion = (guardado as? EstadoUi.Error)?.mensaje ?: (eliminado as? EstadoUi.Error)?.mensaje
    LaunchedEffect(errorOperacion) {
        if (errorOperacion != null) {
            avisos.showSnackbar(errorOperacion)
            vm.descartarErrores()
        }
    }

    if (confirmarEliminar) {
        DialogoConfirmacion(
            titulo = "¿Eliminar producto?",
            mensaje = "Se borrará \"${formulario.nombre}\" del catálogo y también su foto. Esta acción no se puede deshacer.",
            textoConfirmar = "Eliminar",
            peligroso = true,
            alConfirmar = {
                confirmarEliminar = false
                vm.eliminar()
            },
            alCancelar = { confirmarEliminar = false }
        )
    }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = if (formulario.esEdicion) "Editar producto" else "Nuevo producto",
                subtitulo = if (formulario.esEdicion) formulario.nombre else "Completa los datos del producto",
                alVolver = alVolver,
                acciones = {
                    if (formulario.esEdicion) {
                        IconButton(onClick = { confirmarEliminar = true }, enabled = !ocupado) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar producto")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(avisos) }
    ) { relleno ->
        when (val estadoCarga = carga) {
            is EstadoUi.Cargando -> EstadoCargando("Cargando producto…", Modifier.padding(relleno))
            is EstadoUi.Error -> EstadoMensaje(
                icono = Icons.Default.Warning,
                titulo = "No se pudo abrir el producto",
                mensaje = estadoCarga.mensaje,
                textoAccion = "Volver al catálogo",
                alAccion = alVolver,
                modifier = Modifier.padding(relleno)
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(relleno)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TarjetaSeccion {
                    Text("Foto del producto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Elige una imagen de tu galería. Se comprime antes de subirla a Firebase Storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Box {
                        ImagenProducto(
                            url = formulario.imagenNueva ?: formulario.imagenUrl,
                            descripcion = "Foto del producto",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(MaterialTheme.shapes.medium)
                        )
                        FilledTonalButton(
                            onClick = {
                                selectorImagen.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !ocupado,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                        ) {
                            val tieneFoto = formulario.imagenNueva != null || formulario.imagenUrl.isNotBlank()
                            Icon(
                                imageVector = if (tieneFoto) Icons.Default.Edit else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (tieneFoto) "Cambiar foto" else "Elegir de la galería")
                        }
                    }
                    if (formulario.imagenNueva != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Foto nueva lista para subir al guardar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                TarjetaSeccion {
                    Text("Datos del producto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    CampoTexto(
                        valor = formulario.nombre,
                        alCambiar = { texto ->
                            vm.actualizar { it.copy(nombre = texto) }
                            vm.limpiarError(ProductoFormViewModel.CAMPO_NOMBRE)
                        },
                        etiqueta = "Nombre",
                        error = errores.nombre,
                        capitalizacion = KeyboardCapitalization.Sentences,
                        habilitado = !ocupado
                    )
                    CampoTexto(
                        valor = formulario.descripcion,
                        alCambiar = { texto -> vm.actualizar { it.copy(descripcion = texto.take(500)) } },
                        etiqueta = "Descripción (opcional)",
                        lineas = 3,
                        capitalizacion = KeyboardCapitalization.Sentences,
                        habilitado = !ocupado
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Categoría", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Producto.CATEGORIAS) { opcion ->
                            FilterChip(
                                selected = formulario.categoria == opcion,
                                onClick = {
                                    vm.actualizar { it.copy(categoria = opcion) }
                                    vm.limpiarError(ProductoFormViewModel.CAMPO_CATEGORIA)
                                },
                                label = { Text(opcion) },
                                leadingIcon = if (formulario.categoria == opcion) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else {
                                    null
                                },
                                enabled = !ocupado,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                    errores.categoria?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CampoTexto(
                            valor = formulario.precio,
                            alCambiar = { texto ->
                                vm.actualizar { it.copy(precio = texto) }
                                vm.limpiarError(ProductoFormViewModel.CAMPO_PRECIO)
                            },
                            etiqueta = "Precio (S/)",
                            error = errores.precio,
                            tipoTeclado = KeyboardType.Decimal,
                            habilitado = !ocupado,
                            modifier = Modifier.weight(1f)
                        )
                        CampoTexto(
                            valor = formulario.stock,
                            alCambiar = { texto ->
                                vm.actualizar { it.copy(stock = texto.filter(Char::isDigit)) }
                                vm.limpiarError(ProductoFormViewModel.CAMPO_STOCK)
                            },
                            etiqueta = "Stock",
                            error = errores.stock,
                            tipoTeclado = KeyboardType.Number,
                            accionIme = ImeAction.Done,
                            habilitado = !ocupado,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (formulario.esEdicion && formulario.creadoEn > 0L) {
                    Text(
                        text = "Registrado el ${Formatos.fecha(formulario.creadoEn)}. Solo quien lo creó o un administrador puede modificarlo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                BotonPrincipal(
                    texto = if (formulario.esEdicion) "Guardar cambios" else "Publicar producto",
                    alPulsar = vm::guardar,
                    cargando = guardado is EstadoUi.Cargando,
                    habilitado = !ocupado,
                    icono = Icons.Default.Check
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}