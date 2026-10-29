package com.example.retrofit.view.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retrofit.view.componentes.AvisoEnLinea
import com.example.retrofit.view.componentes.BotonPrincipal
import com.example.retrofit.view.componentes.CampoTexto
import com.example.retrofit.view.componentes.PlantillaAutenticacion
import com.example.retrofit.viewmodel.AuthViewModel
import com.example.retrofit.viewmodel.EstadoUi

@Composable
fun RegistroScreen(
    alRegistrado: () -> Unit,
    alVolver: () -> Unit,
    alVerPrivacidad: () -> Unit,
    vm: AuthViewModel = viewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val errores by vm.errores.collectAsStateWithLifecycle()
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var confirmacion by rememberSaveable { mutableStateOf("") }
    var acepta by rememberSaveable { mutableStateOf(false) }
    val foco = LocalFocusManager.current
    val cargando = estado is EstadoUi.Cargando

    LaunchedEffect(estado) {
        if (estado is EstadoUi.Exito) alRegistrado()
    }

    val enviar = {
        foco.clearFocus()
        vm.registrar(nombre, correo, clave, confirmacion, acepta)
    }

    PlantillaAutenticacion(
        titulo = "Crea tu cuenta",
        subtitulo = "Solo te tomará un minuto"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CampoTexto(
                valor = nombre,
                alCambiar = {
                    nombre = it
                    vm.limpiarCampo(AuthViewModel.CAMPO_NOMBRE)
                },
                etiqueta = "Nombre y apellido",
                icono = Icons.Default.Person,
                error = errores.nombre,
                capitalizacion = KeyboardCapitalization.Words,
                habilitado = !cargando
            )
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
                ayuda = "Usa un correo real, lo necesitarás para recuperar tu contraseña",
                tipoTeclado = KeyboardType.Email,
                habilitado = !cargando
            )
            CampoTexto(
                valor = clave,
                alCambiar = {
                    clave = it
                    vm.limpiarCampo(AuthViewModel.CAMPO_CLAVE)
                },
                etiqueta = "Contraseña",
                icono = Icons.Default.Lock,
                error = errores.clave,
                ayuda = "Mínimo 8 caracteres, con letras y números",
                tipoTeclado = KeyboardType.Password,
                esClave = true,
                habilitado = !cargando
            )
            CampoTexto(
                valor = confirmacion,
                alCambiar = {
                    confirmacion = it
                    vm.limpiarCampo(AuthViewModel.CAMPO_CONFIRMACION)
                },
                etiqueta = "Repite la contraseña",
                icono = Icons.Default.Lock,
                error = errores.confirmacion,
                tipoTeclado = KeyboardType.Password,
                accionIme = ImeAction.Done,
                alAccionIme = enviar,
                esClave = true,
                habilitado = !cargando
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !cargando) {
                        acepta = !acepta
                        vm.limpiarCampo(AuthViewModel.CAMPO_PRIVACIDAD)
                    }
            ) {
                Checkbox(
                    checked = acepta,
                    onCheckedChange = {
                        acepta = it
                        vm.limpiarCampo(AuthViewModel.CAMPO_PRIVACIDAD)
                    },
                    enabled = !cargando
                )
                Text(
                    text = "Acepto la política de privacidad de Mi Market",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = alVerPrivacidad) {
                    Text("Leer", fontWeight = FontWeight.Bold)
                }
            }
            errores.privacidad?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            val error = (estado as? EstadoUi.Error)?.mensaje
            if (error != null) {
                Spacer(Modifier.height(4.dp))
                AvisoEnLinea(error)
            }
            Spacer(Modifier.height(12.dp))
            BotonPrincipal(texto = "Crear cuenta", alPulsar = enviar, cargando = cargando)
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = alVolver,
                enabled = !cargando,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("¿Ya tienes cuenta? Inicia sesión")
            }
        }
    }
}
