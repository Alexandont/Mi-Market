package com.example.retrofit.view.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.retrofit.view.componentes.BarraSuperior
import com.example.retrofit.view.componentes.TarjetaSeccion

private data class SeccionPolitica(val titulo: String, val parrafos: List<String>)

private val POLITICA = listOf(
    SeccionPolitica(
        "Qué datos usamos",
        listOf(
            "Para crear tu cuenta pedimos tu nombre, tu correo y una contraseña. La contraseña la gestiona Firebase Authentication y nunca la vemos ni la guardamos nosotros.",
            "Cuando compras guardamos el detalle de cada pedido: productos, cantidades, total, fecha y la dirección de entrega que elegiste."
        )
    ),
    SeccionPolitica(
        "Tu ubicación",
        listOf(
            "Solo pedimos acceso a la ubicación cuando abres el mapa y después de explicarte para qué sirve. La usamos para mostrarte la tienda más cercana y proponerte tu punto de entrega.",
            "La ubicación en tiempo real no sale de tu teléfono y no se rastrea en segundo plano. Lo único que se guarda es la dirección de entrega que confirmas con el botón Guardar.",
            "Si no das el permiso, la app sigue funcionando: puedes marcar tu dirección tocando el mapa."
        )
    ),
    SeccionPolitica(
        "Dónde se guardan",
        listOf(
            "Tus datos se guardan en Google Firebase (Authentication, Cloud Firestore y Cloud Storage). Las reglas de seguridad impiden que otra persona lea tu perfil o tus pedidos.",
            "Las fotos de los productos son públicas para los usuarios de la app porque forman parte del catálogo."
        )
    ),
    SeccionPolitica(
        "Tus derechos",
        listOf(
            "Puedes cambiar tu nombre y tu dirección de entrega cuando quieras desde Mi perfil.",
            "Puedes retirar el permiso de ubicación desde Mi perfil o desde los ajustes de Android.",
            "Puedes eliminar tu cuenta desde Mi perfil. Al hacerlo borramos tu perfil, tu dirección y tu historial de pedidos. Los productos que publicaste siguen en el catálogo porque pertenecen a la tienda."
        )
    ),
    SeccionPolitica(
        "Lo que no hacemos",
        listOf(
            "No vendemos ni compartimos tus datos con terceros para publicidad y no usamos tu ubicación para nada distinto a lo descrito aquí."
        )
    )
)

@Composable
fun PrivacidadScreen(alVolver: () -> Unit) {
    Scaffold(
        topBar = { BarraSuperior(titulo = "Política de privacidad", subtitulo = "Mi Market", alVolver = alVolver) }
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Te contamos en pocas palabras qué datos usa Mi Market, para qué y cómo puedes controlarlos.",
                style = MaterialTheme.typography.bodyLarge
            )
            POLITICA.forEach { seccion ->
                TarjetaSeccion {
                    Text(seccion.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    seccion.parrafos.forEach { parrafo ->
                        Spacer(Modifier.height(8.dp))
                        Text(parrafo, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text(
                text = "Última actualización: octubre de 2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
