package com.example.retrofit.view.navegacion

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.retrofit.model.Coordenada
import com.example.retrofit.view.pantallas.CarritoScreen
import com.example.retrofit.view.pantallas.CatalogoScreen
import com.example.retrofit.view.pantallas.LoginScreen
import com.example.retrofit.view.pantallas.PedidosScreen
import com.example.retrofit.view.pantallas.PerfilScreen
import com.example.retrofit.view.pantallas.PrivacidadScreen
import com.example.retrofit.view.pantallas.ProductoFormScreen
import com.example.retrofit.view.pantallas.RegistroScreen
import com.example.retrofit.view.pantallas.TiendasMapaScreen
import com.example.retrofit.viewmodel.CatalogoViewModel

private fun NavHostController.volver() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    }
}

private fun NavHostController.irAlCatalogo() {
    navigate(Rutas.CATALOGO) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun MiMarketNavHost(inicio: String, alReiniciar: () -> Unit) {
    val nav = rememberNavController()
    val dueno = checkNotNull(LocalViewModelStoreOwner.current)

    NavHost(navController = nav, startDestination = inicio) {
        composable(Rutas.LOGIN) {
            LoginScreen(
                alIngresar = { nav.irAlCatalogo() },
                alCrearCuenta = { nav.navigate(Rutas.REGISTRO) }
            )
        }
        composable(Rutas.REGISTRO) {
            RegistroScreen(
                alRegistrado = { nav.irAlCatalogo() },
                alVolver = { nav.volver() },
                alVerPrivacidad = { nav.navigate(Rutas.PRIVACIDAD) }
            )
        }
        composable(Rutas.CATALOGO) {
            CatalogoScreen(
                vm = viewModel<CatalogoViewModel>(viewModelStoreOwner = dueno),
                alNuevoProducto = { nav.navigate(Rutas.producto()) },
                alEditarProducto = { id -> nav.navigate(Rutas.producto(id)) },
                alVerCarrito = { nav.navigate(Rutas.CARRITO) { launchSingleTop = true } },
                alVerMapa = { nav.navigate(Rutas.mapa()) },
                alVerPerfil = { nav.navigate(Rutas.PERFIL) { launchSingleTop = true } }
            )
        }
        composable(
            route = Rutas.PRODUCTO,
            arguments = listOf(
                navArgument(Rutas.ARG_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entrada ->
            ProductoFormScreen(
                productoId = entrada.arguments?.getString(Rutas.ARG_ID),
                alVolver = { nav.volver() }
            )
        }
        composable(Rutas.CARRITO) {
            CarritoScreen(
                vm = viewModel<CatalogoViewModel>(viewModelStoreOwner = dueno),
                alVolver = { nav.volver() },
                alElegirEntrega = { nav.navigate(Rutas.mapa()) },
                alVerPedidos = {
                    nav.navigate(Rutas.PEDIDOS) {
                        popUpTo(Rutas.CATALOGO)
                    }
                }
            )
        }
        composable(
            route = Rutas.MAPA,
            arguments = listOf(
                navArgument(Rutas.ARG_LAT) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Rutas.ARG_LNG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entrada ->
            val lat = entrada.arguments?.getString(Rutas.ARG_LAT)?.toDoubleOrNull()
            val lng = entrada.arguments?.getString(Rutas.ARG_LNG)?.toDoubleOrNull()
            TiendasMapaScreen(
                enfoque = if (lat != null && lng != null) Coordenada(lat, lng) else null,
                alVolver = { nav.volver() },
                alVerPrivacidad = { nav.navigate(Rutas.PRIVACIDAD) }
            )
        }
        composable(Rutas.PERFIL) {
            PerfilScreen(
                alVolver = { nav.volver() },
                alVerPedidos = { nav.navigate(Rutas.PEDIDOS) },
                alElegirEntrega = { nav.navigate(Rutas.mapa()) },
                alVerPrivacidad = { nav.navigate(Rutas.PRIVACIDAD) },
                alSesionTerminada = alReiniciar
            )
        }
        composable(Rutas.PEDIDOS) {
            PedidosScreen(
                alVolver = { nav.volver() },
                alIrAlCatalogo = { nav.popBackStack(Rutas.CATALOGO, inclusive = false) },
                alVerEnMapa = { punto -> nav.navigate(Rutas.mapa(punto.lat, punto.lng)) }
            )
        }
        composable(Rutas.PRIVACIDAD) {
            PrivacidadScreen(alVolver = { nav.volver() })
        }
    }
}
