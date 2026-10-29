package com.example.retrofit.view.pantallas

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retrofit.model.Coordenada
import com.example.retrofit.model.Tienda
import com.example.retrofit.repository.ListaTiendas
import com.example.retrofit.util.Formatos
import com.example.retrofit.view.componentes.BarraSuperior
import com.example.retrofit.view.componentes.Etiqueta
import com.example.retrofit.viewmodel.EstadoUbicacion
import com.example.retrofit.viewmodel.EstadoUi
import com.example.retrofit.viewmodel.TiendaCercana
import com.example.retrofit.viewmodel.TiendasViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch

private val CENTRO_LIMA = LatLng(-12.0897, -77.0282)

private val PERMISOS_UBICACION = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private fun Coordenada.aLatLng() = LatLng(lat, lng)

private fun Context.tienePermisoUbicacion(): Boolean =
    PERMISOS_UBICACION.any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

private fun Context.buscarActividad(): Activity? {
    var actual: Context? = this
    while (actual is ContextWrapper) {
        if (actual is Activity) return actual
        actual = actual.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiendasMapaScreen(
    enfoque: Coordenada?,
    alVolver: () -> Unit,
    alVerPrivacidad: () -> Unit,
    vm: TiendasViewModel = viewModel()
) {
    val contexto = LocalContext.current
    val tiendas by vm.tiendas.collectAsStateWithLifecycle()
    val ubicacion by vm.ubicacion.collectAsStateWithLifecycle()
    val entrega by vm.entrega.collectAsStateWithLifecycle()
    val direccionEntrega by vm.direccionEntrega.collectAsStateWithLifecycle()
    val guardando by vm.guardandoEntrega.collectAsStateWithLifecycle()
    val mensaje by vm.mensaje.collectAsStateWithLifecycle()
    val perfil by vm.perfil.collectAsStateWithLifecycle()
    val cercanas by vm.tiendasCercanas.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    var mostrarConsentimiento by rememberSaveable { mutableStateOf(false) }
    var permisoConcedido by remember { mutableStateOf(contexto.tienePermisoUbicacion()) }
    var rechazoPermanente by rememberSaveable { mutableStateOf(false) }
    var seguirUbicacion by rememberSaveable { mutableStateOf(enfoque == null) }

    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(enfoque?.aLatLng() ?: CENTRO_LIMA, if (enfoque != null) 16f else 12f)
    }

    val latGuardada = perfil?.latEntrega
    val lngGuardada = perfil?.lngEntrega
    val entregaGuardada = if (latGuardada != null && lngGuardada != null) Coordenada(latGuardada, lngGuardada) else null
    val puntoEntrega = entrega ?: entregaGuardada

    val solicitarPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        val concedido = resultado.values.any { it }
        permisoConcedido = concedido
        if (concedido) {
            rechazoPermanente = false
            seguirUbicacion = true
            vm.obtenerUbicacion()
        } else {
            val actividad = contexto.buscarActividad()
            rechazoPermanente = actividad != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(actividad, Manifest.permission.ACCESS_FINE_LOCATION)
            vm.permisoDenegado()
        }
    }

    val pedirUbicacion: () -> Unit = {
        seguirUbicacion = true
        if (vm.necesitaConsentimiento) {
            mostrarConsentimiento = true
        } else if (permisoConcedido) {
            vm.obtenerUbicacion()
        } else {
            solicitarPermiso.launch(PERMISOS_UBICACION)
        }
    }

    val abrirAjustesApp: () -> Unit = {
        runCatching {
            contexto.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null))
            )
        }
        Unit
    }

    val abrirAjustesGps: () -> Unit = {
        runCatching { contexto.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
        Unit
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val ahora = contexto.tienePermisoUbicacion()
        permisoConcedido = ahora
        val estado = vm.ubicacion.value
        val pendiente = estado is EstadoUbicacion.PermisoDenegado || estado is EstadoUbicacion.GpsDesactivado
        if (ahora && pendiente && !vm.necesitaConsentimiento) {
            rechazoPermanente = false
            vm.obtenerUbicacion()
        }
    }

    LaunchedEffect(ubicacion) {
        val actual = ubicacion
        if (actual is EstadoUbicacion.Obtenida && seguirUbicacion) {
            camara.animate(CameraUpdateFactory.newLatLngZoom(actual.coordenada.aLatLng(), 14f), 900)
        }
    }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            avisos.showSnackbar(it)
            vm.descartarMensaje()
        }
    }

    val enfocarTienda: (Tienda) -> Unit = { tienda ->
        seguirUbicacion = false
        alcance.launch {
            camara.animate(CameraUpdateFactory.newLatLngZoom(tienda.coordenada.aLatLng(), 16f), 800)
        }
    }

    if (mostrarConsentimiento) {
        DialogoConsentimiento(
            alAceptar = {
                mostrarConsentimiento = false
                vm.aceptarConsentimiento()
                if (permisoConcedido) vm.obtenerUbicacion() else solicitarPermiso.launch(PERMISOS_UBICACION)
            },
            alRechazar = {
                mostrarConsentimiento = false
                vm.rechazarConsentimiento()
            },
            alVerPrivacidad = {
                mostrarConsentimiento = false
                alVerPrivacidad()
            }
        )
    }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = "Tiendas y entrega",
                subtitulo = "Toca el mapa para marcar tu punto de entrega",
                alVolver = alVolver,
                acciones = {
                    IconButton(onClick = vm::cargarTiendas) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar tiendas")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(avisos) }
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = relleno.calculateTopPadding())
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val listaBase = (tiendas as? EstadoUi.Exito<ListaTiendas>)?.datos?.tiendas.orEmpty()
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = camara,
                    properties = MapProperties(
                        isMyLocationEnabled = permisoConcedido && ubicacion is EstadoUbicacion.Obtenida
                    ),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = false,
                        zoomControlsEnabled = false,
                        mapToolbarEnabled = false
                    ),
                    onMapClick = { punto ->
                        seguirUbicacion = false
                        vm.seleccionarEntrega(Coordenada(punto.latitude, punto.longitude))
                    }
                ) {
                    listaBase.forEach { tienda ->
                        key(tienda.id) {
                            Marker(
                                state = rememberMarkerState(position = tienda.coordenada.aLatLng()),
                                title = tienda.nombre,
                                snippet = tienda.direccion,
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            )
                        }
                    }
                    if (puntoEntrega != null) {
                        key(puntoEntrega) {
                            Marker(
                                state = rememberMarkerState(position = puntoEntrega.aLatLng()),
                                title = "Tu punto de entrega",
                                snippet = direccionEntrega ?: perfil?.direccionEntrega,
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                                zIndex = 2f
                            )
                        }
                    }
                    if (enfoque != null) {
                        Marker(
                            state = rememberMarkerState(key = "enfoque", position = enfoque.aLatLng()),
                            title = "Entrega de tu pedido",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            zIndex = 1f
                        )
                    }
                }
                FloatingActionButton(
                    onClick = pedirUbicacion,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    if (ubicacion is EstadoUbicacion.Cargando) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    } else {
                        Icon(Icons.Default.LocationOn, contentDescription = "Usar mi ubicación")
                    }
                }
                Leyenda(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shadowElevation = 16.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PanelUbicacion(
                        estado = ubicacion,
                        rechazoPermanente = rechazoPermanente,
                        alPedirUbicacion = pedirUbicacion,
                        alAbrirAjustesApp = abrirAjustesApp,
                        alAbrirAjustesGps = abrirAjustesGps,
                        alUsarComoEntrega = vm::usarMiUbicacionComoEntrega,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    PanelEntrega(
                        hayNueva = entrega != null,
                        direccion = if (entrega != null) direccionEntrega else perfil?.direccionEntrega?.ifBlank { null },
                        hayPunto = puntoEntrega != null,
                        guardando = guardando,
                        alGuardar = vm::guardarEntrega,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    PanelTiendas(
                        estado = tiendas,
                        cercanas = cercanas,
                        esAdmin = perfil?.esAdmin == true,
                        alReintentar = vm::cargarTiendas,
                        alPublicar = vm::publicarTiendasDeEjemplo,
                        alElegir = enfocarTienda
                    )
                }
            }
        }
    }
}

@Composable
private fun Leyenda(modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            FilaLeyenda(MaterialTheme.colorScheme.primary, "Tiendas Mi Market")
            FilaLeyenda(MaterialTheme.colorScheme.secondary, "Tu punto de entrega")
        }
    }
}

@Composable
private fun FilaLeyenda(color: androidx.compose.ui.graphics.Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DialogoConsentimiento(
    alAceptar: () -> Unit,
    alRechazar: () -> Unit,
    alVerPrivacidad: () -> Unit
) {
    AlertDialog(
        onDismissRequest = alRechazar,
        icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Antes de usar tu ubicación", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mi Market quiere usar la ubicación de tu teléfono solo para:")
                Text("• Mostrarte qué tienda está más cerca de ti.")
                Text("• Proponerte tu dirección de entrega en el mapa.")
                Text(
                    text = "No rastreamos tu ubicación en segundo plano ni la compartimos con terceros. Solo guardamos la dirección de entrega que tú confirmes, y puedes retirar este permiso cuando quieras desde tu perfil.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = alVerPrivacidad, contentPadding = PaddingValues(0.dp)) {
                    Text("Leer la política de privacidad")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = alAceptar) {
                Text("Aceptar y continuar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = alRechazar) {
                Text("Ahora no")
            }
        }
    )
}

@Composable
private fun PanelUbicacion(
    estado: EstadoUbicacion,
    rechazoPermanente: Boolean,
    alPedirUbicacion: () -> Unit,
    alAbrirAjustesApp: () -> Unit,
    alAbrirAjustesGps: () -> Unit,
    alUsarComoEntrega: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (estado) {
        is EstadoUbicacion.SinSolicitar -> TarjetaEstado(
            icono = Icons.Default.LocationOn,
            titulo = "Encuentra tu tienda más cercana",
            texto = "Usa tu ubicación o toca el mapa para marcar tu dirección.",
            textoAccion = "Usar mi ubicación",
            alAccion = alPedirUbicacion,
            modifier = modifier
        )
        is EstadoUbicacion.Cargando -> Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            Spacer(Modifier.width(12.dp))
            Text("Buscando tu ubicación…")
        }
        is EstadoUbicacion.Obtenida -> TarjetaEstado(
            icono = Icons.Default.Place,
            titulo = "Estás aquí",
            texto = estado.direccion ?: Formatos.coordenadas(estado.coordenada.lat, estado.coordenada.lng),
            textoAccion = "Usar como entrega",
            alAccion = alUsarComoEntrega,
            modifier = modifier
        )
        is EstadoUbicacion.PermisoDenegado -> TarjetaEstado(
            icono = Icons.Default.Info,
            titulo = "Sin acceso a tu ubicación",
            texto = if (rechazoPermanente) {
                "El permiso está bloqueado. Puedes activarlo en los ajustes del teléfono o seguir marcando tu dirección tocando el mapa."
            } else {
                "No pasa nada: toca el mapa para marcar tu dirección de entrega o elige una tienda de la lista."
            },
            textoAccion = if (rechazoPermanente) "Abrir ajustes" else "Intentar de nuevo",
            alAccion = if (rechazoPermanente) alAbrirAjustesApp else alPedirUbicacion,
            advertencia = true,
            modifier = modifier
        )
        is EstadoUbicacion.GpsDesactivado -> TarjetaEstado(
            icono = Icons.Default.Warning,
            titulo = "El GPS está desactivado",
            texto = "Actívalo para ubicarte, o marca tu dirección tocando el mapa.",
            textoAccion = "Activar GPS",
            alAccion = alAbrirAjustesGps,
            advertencia = true,
            modifier = modifier
        )
        is EstadoUbicacion.Error -> TarjetaEstado(
            icono = Icons.Default.Warning,
            titulo = "No pudimos ubicarte",
            texto = estado.mensaje,
            textoAccion = "Reintentar",
            alAccion = alPedirUbicacion,
            advertencia = true,
            modifier = modifier
        )
    }
}

@Composable
private fun TarjetaEstado(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    texto: String,
    textoAccion: String,
    alAccion: () -> Unit,
    modifier: Modifier = Modifier,
    advertencia: Boolean = false
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (advertencia) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = if (advertencia) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(texto, style = MaterialTheme.typography.bodySmall)
            }
        }
        TextButton(
            onClick = alAccion,
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 8.dp, bottom = 4.dp)
        ) {
            Text(textoAccion, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PanelEntrega(
    hayNueva: Boolean,
    direccion: String?,
    hayPunto: Boolean,
    guardando: Boolean,
    alGuardar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (hayNueva) "Nuevo punto de entrega" else "Dirección de entrega",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = when {
                !hayPunto -> "Aún no eliges dónde recibir tus pedidos. Toca el mapa para marcarlo."
                direccion == null -> "Buscando la dirección del punto marcado…"
                else -> direccion
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (hayNueva) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = alGuardar,
                enabled = !guardando,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (guardando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar como mi dirección", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PanelTiendas(
    estado: EstadoUi<ListaTiendas>,
    cercanas: List<TiendaCercana>,
    esAdmin: Boolean,
    alReintentar: () -> Unit,
    alPublicar: () -> Unit,
    alElegir: (Tienda) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nuestras tiendas",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            val deEjemplo = (estado as? EstadoUi.Exito<ListaTiendas>)?.datos?.sonDeEjemplo == true
            if (deEjemplo) {
                Etiqueta(
                    texto = "Datos de ejemplo",
                    fondo = MaterialTheme.colorScheme.secondaryContainer,
                    colorTexto = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        when (estado) {
            is EstadoUi.Inactivo, is EstadoUi.Cargando -> Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Cargando tiendas…")
            }
            is EstadoUi.Error -> Column(Modifier.padding(horizontal = 16.dp)) {
                Text(estado.mensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = alReintentar) { Text("Reintentar") }
            }
            is EstadoUi.Exito -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cercanas, key = { it.tienda.id }) { item ->
                        TarjetaTienda(item = item, esLaMasCercana = item == cercanas.firstOrNull() && item.distanciaKm != null, alElegir = alElegir)
                    }
                }
                if (esAdmin && estado.datos.sonDeEjemplo) {
                    OutlinedButton(
                        onClick = alPublicar,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Publicar estas tiendas en Firestore")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaTienda(item: TiendaCercana, esLaMasCercana: Boolean, alElegir: (Tienda) -> Unit) {
    Card(
        onClick = { alElegir(item.tienda) },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.width(250.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.tienda.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                item.distanciaKm?.let {
                    Etiqueta(
                        texto = Formatos.distancia(it),
                        fondo = MaterialTheme.colorScheme.primary,
                        colorTexto = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            if (esLaMasCercana) {
                Text(
                    text = "La más cercana a ti",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.tienda.direccion,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.tienda.horario,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.tienda.telefono.isNotBlank()) {
                Text(
                    text = "Tel. ${item.tienda.telefono}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
