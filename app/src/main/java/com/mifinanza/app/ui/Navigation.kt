package com.mifinanza.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.mifinanza.app.R
import com.mifinanza.app.data.currentYearMonth
import com.mifinanza.app.ui.components.NotificationOverlay
import com.mifinanza.app.ui.screens.*
import com.mifinanza.app.ui.theme.*
import com.mifinanza.app.viewmodel.AppViewModel
import java.util.Calendar

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard    : Screen("dashboard",    "Inicio",   Icons.Default.Home)
    object Movimientos  : Screen("movimientos",  "Dinero",   Icons.Default.SwapHoriz)
    object Suscripciones: Screen("suscripciones","Suscr.",   Icons.Default.Repeat)
    object MSI          : Screen("msi",          "MSI",      Icons.Default.Autorenew)
    object Prestamos    : Screen("prestamos",    "Préstamos",Icons.Default.Handshake)
    object Cuentas      : Screen("cuentas",      "Cuentas",  Icons.Default.AccountBalanceWallet)
}

val NAV_ITEMS = listOf(
    Screen.Dashboard, Screen.Movimientos, Screen.Suscripciones, Screen.MSI, Screen.Prestamos
)

fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when { h < 12 -> "Buenos días"; h < 19 -> "Buenas tardes"; else -> "Buenas noches" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    // isLoaded evita el flash de WelcomeScreen antes de que DataStore cargue
    var isLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(state) { isLoaded = true }

    if (!isLoaded) {
        // Splash mientras carga DataStore
        Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFF0F1926)),
            contentAlignment = androidx.compose.ui.Alignment.Center) {}
        return
    }

    if (state.accounts.isEmpty() && state.userName.isEmpty()) {
        WelcomeScreen(vm = vm)
        return
    }

    val nav = rememberNavController()
    val backstackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backstackEntry?.destination?.route
    val isHome = currentRoute == Screen.Dashboard.route
    val firstName = state.userName.trim().split(" ").firstOrNull()?.takeIf { it.isNotEmpty() } ?: ""
    val initials = state.userName.trim().split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").ifEmpty { "M" }

    val pageTitle = when (currentRoute) {
        Screen.Dashboard.route     -> "Monet"
        Screen.Movimientos.route   -> "Movimientos"
        Screen.Suscripciones.route -> "Suscripciones"
        Screen.MSI.route           -> "A Meses"
        Screen.Prestamos.route     -> "Préstamos"
        Screen.Cuentas.route       -> "Mis Cuentas"
        else                       -> "Monet"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // ── PREMIUM TOP BAR ──────────────────────────────────────────────
            Surface(
                color = if (isHome) Color(0xFF0F1926) else MaterialTheme.colorScheme.surface,
                shadowElevation = if (isHome) 0.dp else 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.monet_logo),
                        contentDescription = "Monet",
                        modifier = Modifier.size(34.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(10.dp))

                    // Title / Greeting
                    Column(modifier = Modifier.weight(1f)) {
                        if (isHome && firstName.isNotEmpty()) {
                            Text("${greeting()},", fontSize = 11.sp, color = Color(0xFF8B95A8), fontWeight = FontWeight.Medium)
                            Text(firstName, fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (isHome) Color.White else MaterialTheme.colorScheme.onSurface)
                        } else {
                            Text(pageTitle, fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (isHome) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Cuentas wallet icon
                        IconButton(onClick = {
                            nav.navigate(Screen.Cuentas.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        }, modifier = Modifier.size(38.dp)) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = "Cuentas",
                                tint = if (currentRoute == Screen.Cuentas.route) GreenBrand else (if (isHome) Color(0xFF8B95A8) else MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Dark/light mode — SINGLE toggle, only here
                        IconButton(
                            onClick = { vm.toggleDarkMode() },
                            modifier = Modifier.size(38.dp).clip(CircleShape)
                                .then(if (isHome) Modifier.padding(0.dp) else Modifier)
                        ) {
                            Icon(
                                if (state.darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Modo",
                                tint = if (isHome) Color(0xFF8B95A8) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Avatar with initials (only on home)
                        if (isHome) {
                            Box(
                                modifier = Modifier.size(34.dp).clip(CircleShape)
                                    .then(Modifier.padding(0.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(androidx.compose.ui.graphics.Brush.linearGradient(listOf(GreenBrand, GreenBrand.copy(.7f))))
                                }
                                Text(initials, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF021A12))
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                NAV_ITEMS.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(screen.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label, modifier = Modifier.size(22.dp)) },
                        label = { Text(screen.label, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        alwaysShowLabel = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GreenBrand, selectedTextColor = GreenBrand,
                            indicatorColor = GreenBrand.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(
                navController = nav,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(180)) + slideInVertically { 20 } },
                exitTransition = { fadeOut(tween(180)) }
            ) {
                composable(Screen.Dashboard.route)     { DashboardScreen(vm) }
                composable(Screen.Movimientos.route)   { MovimientosScreen(vm) }
                composable(Screen.Suscripciones.route) { SuscripcionesScreen(vm) }
                composable(Screen.MSI.route)           { MSIScreen(vm) }
                composable(Screen.Prestamos.route)     { PrestamosScreen(vm) }
                composable(Screen.Cuentas.route)       { CuentasScreen(vm) }
            }
            // Global in-app notification overlay at top
            Box(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 8.dp)) {
                NotificationOverlay()
            }
        }
    }
}
