package com.example.retrofit.view.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.retrofit.model.Producto
import com.example.retrofit.util.Formatos
import com.example.retrofit.view.componentes.BarraSuperior
import com.example.retrofit.view.componentes.EstadoCargando
import com.example.retrofit.view.componentes.EstadoMensaje
import com.example.retrofit.view.componentes.Etiqueta
import com.example.retrofit.view.componentes.ImagenProducto
import com.example.retrofit.view.componentes.SelectorCantidad
import com.example.retrofit.viewmodel.CatalogoViewModel
import com.example.retrofit.viewmodel.EstadoUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    vm: CatalogoViewModel,
    alNuevoProducto: () -> Unit,
    alEditarProducto: (String) -> Unit,
    alVerCarrito: () -> Unit,
    alVerMapa: () -> Unit,
    alVerPerfil: () -> Unit
) {
    val catalogo by vm.catalogo.collectAsStateWithLifecycle()
    val busqueda by vm.busqueda.collectAsStateWithLifecycle()
    val categoria by vm.categoria.collectAsStateWithLifecycle()
    val carrito by vm.carrito.collectAsStateWithLifecycle()
    val enCarrito by vm.itemsCarrito.collectAsStateWithLifecycle()
    val perfil by vm.perfil.collectAsStateWithLifecycle()
    val mensaje by vm.mensaje.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    val unidades = enCarrito.sumOf { it.cantidad }
    val total = enCarrito.sumOf { it.subtotal }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            avisos.showSnackbar(it)
            vm.descartarMensaje()
        }
    }

    val nombre = perfil?.nombre?.substringBefore(' ').orEmpty()

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = "Mi Market",
                subtitulo = if (nombre.isNotBlank()) "Hola, $nombre" else "Catálogo de productos",
                acciones = {
                    IconButton(onClick = alVerMapa) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Tiendas y entrega")
                    }
                    IconButton(onClick = alVerCarrito) {
                        BadgedBox(badge = {
                            if (unidades > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                    Text("$unidades")
                                }
                            }
                        }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito")
                        }
                    }
                    IconButton(onClick = alVerPerfil) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Mi perfil")
                    }
                }
            )
        },
        floatingActionButton = {
            if (enCarrito.isEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = alNuevoProducto,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nuevo producto") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                SmallFloatingActionButton(
                    onClick = alNuevoProducto,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo producto")
                }
            }
        },
        bottomBar = {
            if (enCarrito.isNotEmpty()) {
                ResumenCarrito(unidades = unidades, total = total, alVerCarrito = alVerCarrito)
            }
        },
        snackbarHost = { SnackbarHost(avisos) }
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
        ) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = vm::buscar,
                placeholder = { Text("Buscar productos") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (busqueda.isNotEmpty()) {
                        IconButton(onClick = { vm.buscar("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = categoria == null,
                        onClick = { vm.elegirCategoria(null) },
                        label = { Text("Todas") },
                        colors = coloresChip()
                    )
                }
                items(Producto.CATEGORIAS) { opcion ->
                    FilterChip(
                        selected = categoria == opcion,
                        onClick = { vm.elegirCategoria(opcion) },
                        label = { Text(opcion) },
                        colors = coloresChip()
                    )
                }
            }
            when (val estado = catalogo) {
                is EstadoUi.Inactivo, is EstadoUi.Cargando -> EstadoCargando("Cargando catálogo…")
                is EstadoUi.Error -> EstadoMensaje(
                    icono = Icons.Default.Warning,
                    titulo = "No pudimos cargar el catálogo",
                    mensaje = estado.mensaje
                )
                is EstadoUi.Exito -> {
                    val filtrando = busqueda.isNotBlank() || categoria != null
                    if (estado.datos.isEmpty()) {
                        if (filtrando) {
                            EstadoMensaje(
                                icono = Icons.Default.Search,
                                titulo = "Sin resultados",
                                mensaje = "No encontramos productos con ese filtro. Prueba con otra palabra o categoría.",
                                textoAccion = "Quitar filtros",
                                alAccion = {
                                    vm.buscar("")
                                    vm.elegirCategoria(categoria)
                                }
                            )
                        } else {
                            EstadoMensaje(
                                icono = Icons.Default.ShoppingCart,
                                titulo = "El catálogo está vacío",
                                mensaje = "Registra el primer producto con su foto, precio y stock.",
                                textoAccion = "Agregar producto",
                                alAccion = alNuevoProducto
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(estado.datos, key = { it.id }) { producto ->
                                TarjetaProducto(
                                    producto = producto,
                                    cantidad = carrito[producto.id] ?: 0,
                                    editable = vm.puedeEditar(producto),
                                    alAgregar = { vm.agregar(producto) },
                                    alQuitar = { vm.quitar(producto) },
                                    alEditar = { alEditarProducto(producto.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun coloresChip() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaProducto(
    producto: Producto,
    cantidad: Int,
    editable: Boolean,
    alAgregar: () -> Unit,
    alQuitar: () -> Unit,
    alEditar: () -> Unit
) {
    val agotado = producto.stock <= 0
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = editable, onClick = alEditar)
    ) {
        Box {
            ImagenProducto(
                url = producto.imagenUrl,
                descripcion = producto.nombre,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
            )
            if (producto.categoria.isNotBlank()) {
                Etiqueta(
                    texto = producto.categoria,
                    fondo = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    colorTexto = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }
            if (editable) {
                Surface(
                    onClick = alEditar,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar ${producto.nombre}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(
                text = producto.nombre,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Formatos.soles(producto.precio),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (agotado) "Agotado" else "Stock ${producto.stock}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (agotado) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            if (cantidad > 0) {
                SelectorCantidad(
                    cantidad = cantidad,
                    alQuitar = alQuitar,
                    alAgregar = alAgregar,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Button(
                    onClick = alAgregar,
                    enabled = !agotado,
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Agregar", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun ResumenCarrito(unidades: Int, total: Double, alVerCarrito: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (unidades == 1) "1 producto" else "$unidades productos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = Formatos.soles(total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Button(
                onClick = alVerCarrito,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .height(48.dp)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Ver carrito", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
            }
        }
    }
}
