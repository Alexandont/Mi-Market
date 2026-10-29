package com.example.retrofit.view.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retrofit.model.Coordenada
import com.example.retrofit.model.Pedido
import com.example.retrofit.util.Formatos
import com.example.retrofit.view.componentes.BarraSuperior
import com.example.retrofit.view.componentes.EstadoCargando
import com.example.retrofit.view.componentes.EstadoMensaje
import com.example.retrofit.view.componentes.Etiqueta
import com.example.retrofit.view.componentes.TarjetaSeccion
import com.example.retrofit.viewmodel.EstadoUi
import com.example.retrofit.viewmodel.PerfilViewModel

@Composable
fun PedidosScreen(
    alVolver: () -> Unit,
    alIrAlCatalogo: () -> Unit,
    alVerEnMapa: (Coordenada) -> Unit,
    vm: PerfilViewModel = viewModel()
) {
    val pedidos by vm.pedidos.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { BarraSuperior(titulo = "Mis pedidos", subtitulo = "Historial de compras", alVolver = alVolver) }
    ) { relleno ->
        when (val estado = pedidos) {
            is EstadoUi.Inactivo, is EstadoUi.Cargando -> EstadoCargando("Cargando tus pedidos…", Modifier.padding(relleno))
            is EstadoUi.Error -> EstadoMensaje(
                icono = Icons.Default.Warning,
                titulo = "No pudimos cargar tus pedidos",
                mensaje = estado.mensaje,
                modifier = Modifier.padding(relleno)
            )
            is EstadoUi.Exito -> {
                if (estado.datos.isEmpty()) {
                    EstadoMensaje(
                        icono = Icons.AutoMirrored.Filled.List,
                        titulo = "Aún no tienes pedidos",
                        mensaje = "Cuando confirmes tu primera compra la verás aquí con su detalle.",
                        textoAccion = "Ir al catálogo",
                        alAccion = alIrAlCatalogo,
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
                        items(estado.datos, key = { it.id }) { pedido ->
                            TarjetaPedido(pedido = pedido, alVerEnMapa = alVerEnMapa)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaPedido(pedido: Pedido, alVerEnMapa: (Coordenada) -> Unit) {
    val lat = pedido.latEntrega
    val lng = pedido.lngEntrega
    TarjetaSeccion {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Pedido #${pedido.id.take(6).uppercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Formatos.fecha(pedido.fecha),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Etiqueta(
                texto = pedido.estado,
                fondo = MaterialTheme.colorScheme.primaryContainer,
                colorTexto = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.height(10.dp))
        pedido.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = "${item.cantidad} × ${item.nombre}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(text = Formatos.soles(item.subtotal), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Total · ${pedido.cantidadProductos} productos",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = Formatos.soles(pedido.total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (pedido.direccionEntrega.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = pedido.direccionEntrega,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (lat != null && lng != null) {
                    TextButton(onClick = { alVerEnMapa(Coordenada(lat, lng)) }) {
                        Text("Ver en mapa", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
