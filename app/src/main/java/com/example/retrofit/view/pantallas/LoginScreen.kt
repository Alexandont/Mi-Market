package com.example.retrofit.view.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retrofit.view.componentes.AvisoEnLinea
import com.example.retrofit.view.componentes.BotonPrincipal
import com.example.retrofit.view.componentes.BotonSecundario
import com.example.retrofit.view.componentes.CampoTexto
import com.example.retrofit.view.componentes.PlantillaAutenticacion
import com.example.retrofit.viewmodel.AuthViewModel
import com.example.retrofit.viewmodel.EstadoUi

@Composable
fun LoginScreen(
    alIngresar: () -> Unit,
    alCrearCuenta: () -> Unit,
    vm: AuthViewModel = viewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val errores by vm.errores.collectAsStateWithLifecycle()
    val aviso by vm.aviso.collectAsStateWithLifecycle()
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    val foco = LocalFocusManager.current
    val cargando = estado is EstadoUi.Cargando

    LaunchedEffect(estado) {
        if (estado is EstadoUi.Exito) alIngresar()
    }

    PlantillaAutenticacion(
        titulo = "Inicia sesión",
        subtitulo = "Ingresa con tu correo para ver el catálogo y tus pedidos"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CampoTexto(
                valor = correo,
                alCambiar = {
                    correo = it
                    vm.limpiarCampo(AuthViewModel.CAMPO_CORREO)
                    vm.descartarMensajes()
                },
                etiqueta = "Correo electrónico",
                icono = Icons.Default.Email,
                error = errores.correo,
                tipoTeclado = KeyboardType.Email,
                habilitado = !cargando
            )
            CampoTexto(
                valor = clave,
                alCambiar = {
                    clave = it
                    vm.limpiarCampo(AuthViewModel.CAMPO_CLAVE)
                    vm.descartarMensajes()
                },
                etiqueta = "Contraseña",
                icono = Icons.Default.Lock,
                error = errores.clave,
                tipoTeclado = KeyboardType.Password,
                accionIme = ImeAction.Done,
                alAccionIme = {
                    foco.clearFocus()
                    vm.iniciarSesion(correo, clave)
                },
                esClave = true,
                habilitado = !cargando
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { vm.recuperarClave(correo) }, enabled = !cargando) {
                    Text("¿Olvidaste tu contraseña?")
                }
            }
            val error = (estado as? EstadoUi.Error)?.mensaje
            if (error != null) {
                AvisoEnLinea(error)
                Spacer(Modifier.height(8.dp))
            }
            aviso?.let {
                AvisoEnLinea(it, esError = false)
                Spacer(Modifier.height(8.dp))
            }
            BotonPrincipal(
                texto = "Iniciar sesión",
                alPulsar = {
                    foco.clearFocus()
                    vm.iniciarSesion(correo, clave)
                },
                cargando = cargando
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f))
                Text(
                    text = "  ¿Eres nuevo?  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            BotonSecundario(
                texto = "Crear una cuenta",
                alPulsar = alCrearCuenta,
                icono = Icons.Default.Person
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tu sesión se mantiene abierta en este dispositivo hasta que la cierres desde tu perfil.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
