package com.example.retrofit.view.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.retrofit.model.ItemCarrito
import com.example.retrofit.model.Usuario
import com.example.retrofit.util.Formatos
import com.example.retrofit.view.componentes.BarraSuperior
import com.example.retrofit.view.componentes.BotonPrincipal
import com.example.retrofit.view.componentes.EstadoMensaje
import com.example.retrofit.view.componentes.ImagenProducto
import com.example.retrofit.view.componentes.SelectorCantidad
import com.example.retrofit.viewmodel.CatalogoViewModel
import com.example.retrofit.viewmodel.EstadoUi

@Composable
fun CarritoScreen(
    vm: CatalogoViewModel,
    alVolver: () -> Unit,
    alElegirEntrega: () -> Unit,
    alVerPedidos: () -> Unit
) {
    val enCarrito by vm.itemsCarrito.collectAsStateWithLifecycle()
    val perfil by vm.perfil.collectAsStateWithLifecycle()
    val pedido by vm.pedido.collectAsStateWithLifecycle()
    val mensaje by vm.mensaje.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    val total = enCarrito.sumOf { it.subtotal }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            avisos.showSnackbar(it)
            vm.descartarMensaje()
        }
    }

    val errorPedido = (pedido as? EstadoUi.Error)?.mensaje
    LaunchedEffect(errorPedido) {
        if (errorPedido != null) {
            avisos.showSnackbar(errorPedido)
            vm.descartarPedido()
        }
    }

    val confirmado = (pedido as? EstadoUi.Exito<String>)?.datos
    if (confirmado != null) {
        AlertDialog(
            onDismissRequest = {
                vm.descartarPedido()
                alVolver()
            },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("¡Pedido registrado!", fontWeight = FontWeight.Bold) },
            text = {
                Text("Tu pedido #${confirmado.take(6).uppercase()} quedó guardado. Puedes revisarlo cuando quieras en Mis pedidos.")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.descartarPedido()
                    alVerPedidos()
                }) {
                    Text("Ver mis pedidos", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.descartarPedido()
                    alVolver()
                }) {
                    Text("Seguir comprando")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = "Mi carrito",
                subtitulo = if (enCarrito.isEmpty()) null else "${enCarrito.sumOf { it.cantidad }} productos",
                alVolver = alVolver
            )
        },
        bottomBar = {
            if (enCarrito.isNotEmpty()) {
                Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total a pagar", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = Formatos.soles(total),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Pagas al recibir tu pedido",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        BotonPrincipal(
                            texto = "Confirmar pedido",
                            alPulsar = vm::confirmarPedido,
                            cargando = pedido is EstadoUi.Cargando
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(avisos) }
    ) { relleno ->
        if (enCarrito.isEmpty()) {
            EstadoMensaje(
                icono = Icons.Default.ShoppingCart,
                titulo = "Tu carrito está vacío",
                mensaje = "Agrega productos desde el catálogo y aparecerán aquí.",
                textoAccion = "Ir al catálogo",
                alAccion = alVolver,
                modifier = Modifier.padding(relleno)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(relleno),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TarjetaEntrega(perfil = perfil, alElegirEntrega = alElegirEntrega)
                }
                items(enCarrito, key = { it.producto.id }) { item ->
                    FilaCarrito(
                        item = item,
                        alAgregar = { vm.agregar(item.producto) },
                        alQuitar = { vm.quitar(item.producto) },
                        alEliminar = { vm.eliminarDelCarrito(item.producto) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaEntrega(perfil: Usuario?, alElegirEntrega: () -> Unit) {
    val tiene = perfil?.tieneEntrega == true
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (tiene) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (tiene) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (tiene) "Entregar en" else "¿Dónde te lo llevamos?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (tiene) perfil?.direccionEntrega.orEmpty() else "Marca tu dirección en el mapa para poder confirmar.",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = alElegirEntrega) {
                Text(if (tiene) "Cambiar" else "Elegir", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FilaCarrito(
    item: ItemCarrito,
    alAgregar: () -> Unit,
    alQuitar: () -> Unit,
    alEliminar: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImagenProducto(
                url = item.producto.imagenUrl,
                descripcion = item.producto.nombre,
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.producto.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${Formatos.soles(item.producto.precio)} c/u",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                SelectorCantidad(cantidad = item.cantidad, alQuitar = alQuitar, alAgregar = alAgregar)
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = alEliminar) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Quitar ${item.producto.nombre}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = Formatos.soles(item.subtotal),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
