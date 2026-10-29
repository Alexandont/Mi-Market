package com.example.retrofit.view.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retrofit.model.Pedido
import com.example.retrofit.model.Usuario
import com.example.retrofit.util.Formatos
import com.example.retrofit.view.componentes.BarraSuperior
import com.example.retrofit.view.componentes.BotonSecundario
import com.example.retrofit.view.componentes.CampoTexto
import com.example.retrofit.view.componentes.DialogoConfirmacion
import com.example.retrofit.view.componentes.EstadoCargando
import com.example.retrofit.view.componentes.EstadoMensaje
import com.example.retrofit.view.componentes.Etiqueta
import com.example.retrofit.view.componentes.TarjetaSeccion
import com.example.retrofit.viewmodel.EstadoUi
import com.example.retrofit.viewmodel.PerfilViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    alVolver: () -> Unit,
    alVerPedidos: () -> Unit,
    alElegirEntrega: () -> Unit,
    alVerPrivacidad: () -> Unit,
    alSesionTerminada: () -> Unit,
    vm: PerfilViewModel = viewModel()
) {
    val perfil by vm.perfil.collectAsStateWithLifecycle()
    val pedidos by vm.pedidos.collectAsStateWithLifecycle()
    val consentimiento by vm.consentimiento.collectAsStateWithLifecycle()
    val mensaje by vm.mensaje.collectAsStateWithLifecycle()
    val procesando by vm.procesando.collectAsStateWithLifecycle()
    val eliminacion by vm.eliminacion.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    var mostrarEliminar by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            avisos.showSnackbar(it)
            vm.descartarMensaje()
        }
    }

    LaunchedEffect(eliminacion) {
        if (eliminacion is EstadoUi.Exito) alSesionTerminada()
    }

    if (confirmarSalida) {
        DialogoConfirmacion(
            titulo = "¿Cerrar sesión?",
            mensaje = "Tendrás que volver a ingresar tu correo y contraseña. Tu carrito actual se vaciará.",
            textoConfirmar = "Cerrar sesión",
            alConfirmar = {
                confirmarSalida = false
                vm.cerrarSesion()
                alSesionTerminada()
            },
            alCancelar = { confirmarSalida = false }
        )
    }

    if (mostrarEliminar) {
        DialogoEliminarCuenta(
            estado = eliminacion,
            alConfirmar = vm::eliminarCuenta,
            alCancelar = {
                mostrarEliminar = false
                vm.descartarEliminacion()
            }
        )
    }

    Scaffold(
        topBar = { BarraSuperior(titulo = "Mi perfil", alVolver = alVolver) },
        snackbarHost = { SnackbarHost(avisos) }
    ) { relleno ->
        when (val estado = perfil) {
            is EstadoUi.Inactivo, is EstadoUi.Cargando -> EstadoCargando("Cargando tu perfil…", Modifier.padding(relleno))
            is EstadoUi.Error -> EstadoMensaje(
                icono = Icons.Default.Warning,
                titulo = "No pudimos cargar tu perfil",
                mensaje = estado.mensaje,
                modifier = Modifier.padding(relleno)
            )
            is EstadoUi.Exito -> {
                val usuario = estado.datos ?: Usuario(email = vm.correo)
                val cantidadPedidos = (pedidos as? EstadoUi.Exito<List<Pedido>>)?.datos?.size
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(relleno)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Encabezado(usuario)
                    SeccionNombre(
                        nombreActual = usuario.nombre,
                        procesando = procesando,
                        alGuardar = vm::actualizarNombre
                    )
                    TarjetaSeccion {
                        Opcion(
                            icono = Icons.AutoMirrored.Filled.List,
                            titulo = "Mis pedidos",
                            detalle = when (cantidadPedidos) {
                                null -> "Historial de compras"
                                0 -> "Aún no tienes pedidos"
                                1 -> "1 pedido realizado"
                                else -> "$cantidadPedidos pedidos realizados"
                            },
                            alPulsar = alVerPedidos
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Opcion(
                            icono = Icons.Default.Home,
                            titulo = "Dirección de entrega",
                            detalle = usuario.direccionEntrega.ifBlank { "Sin dirección guardada" },
                            alPulsar = alElegirEntrega
                        )
                    }
                    TarjetaSeccion {
                        Text("Privacidad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Usar mi ubicación", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (consentimiento) {
                                        "Activo. Se usa solo para la tienda más cercana y tu punto de entrega."
                                    } else {
                                        "Desactivado. Te preguntaremos antes de volver a usarla."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = consentimiento, onCheckedChange = vm::cambiarConsentimiento)
                        }
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = alVerPrivacidad) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Leer la política de privacidad")
                        }
                    }
                    BotonSecundario(
                        texto = "Cerrar sesión",
                        alPulsar = { confirmarSalida = true },
                        icono = Icons.AutoMirrored.Filled.ExitToApp
                    )
                    BotonSecundario(
                        texto = "Eliminar mi cuenta y mis datos",
                        alPulsar = { mostrarEliminar = true },
                        icono = Icons.Default.Delete,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (usuario.creadoEn > 0L) {
                        Text(
                            text = "Miembro desde ${Formatos.fecha(usuario.creadoEn)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Encabezado(usuario: Usuario) {
    TarjetaSeccion {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Formatos.iniciales(usuario.nombre.ifBlank { usuario.email }),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = usuario.nombre.ifBlank { "Cliente Mi Market" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = usuario.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Etiqueta(
                    texto = if (usuario.esAdmin) "Administrador" else "Cliente",
                    fondo = if (usuario.esAdmin) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                    colorTexto = if (usuario.esAdmin) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SeccionNombre(
    nombreActual: String,
    procesando: Boolean,
    alGuardar: (String) -> String?
) {
    var nombre by rememberSaveable(nombreActual) { mutableStateOf(nombreActual) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val cambiado = nombre.trim() != nombreActual
    TarjetaSeccion {
        Text("Datos personales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        CampoTexto(
            valor = nombre,
            alCambiar = {
                nombre = it
                error = null
            },
            etiqueta = "Nombre y apellido",
            icono = Icons.Default.Person,
            error = error,
            capitalizacion = KeyboardCapitalization.Words,
            accionIme = ImeAction.Done,
            alAccionIme = {
                if (cambiado) {
                    error = alGuardar(nombre)
                }
            },
            habilitado = !procesando
        )
        if (cambiado) {
            TextButton(
                onClick = { error = alGuardar(nombre) },
                enabled = !procesando,
                modifier = Modifier.align(Alignment.End)
            ) {
                if (procesando) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar nombre", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Opcion(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    alPulsar: () -> Unit
) {
    ListItem(
        headlineContent = { Text(titulo, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(detalle, maxLines = 2) },
        leadingContent = { Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = alPulsar)
    )
}

@Composable
private fun DialogoEliminarCuenta(
    estado: EstadoUi<Unit>,
    alConfirmar: (String) -> Unit,
    alCancelar: () -> Unit
) {
    var clave by rememberSaveable { mutableStateOf("") }
    val cargando = estado is EstadoUi.Cargando
    AlertDialog(
        onDismissRequest = { if (!cargando) alCancelar() },
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Eliminar tu cuenta", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Se borrarán de forma permanente tu cuenta, tus datos de perfil, tu dirección de entrega y todo tu historial de pedidos.")
                Text(
                    text = "Por seguridad, escribe tu contraseña para confirmar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CampoTexto(
                    valor = clave,
                    alCambiar = { clave = it },
                    etiqueta = "Contraseña",
                    icono = Icons.Default.Lock,
                    error = (estado as? EstadoUi.Error)?.mensaje,
                    tipoTeclado = KeyboardType.Password,
                    accionIme = ImeAction.Done,
                    alAccionIme = { alConfirmar(clave) },
                    esClave = true,
                    habilitado = !cargando
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { alConfirmar(clave) }, enabled = !cargando) {
                if (cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Eliminar definitivamente", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar, enabled = !cargando) {
                Text("Cancelar")
            }
        }
    )
}
