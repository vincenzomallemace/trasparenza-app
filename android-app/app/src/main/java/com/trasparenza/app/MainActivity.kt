package com.trasparenza.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.trasparenza.app.ui.receipt.ReceiptScannerScreen
import com.trasparenza.app.ui.receipt.SustainabilityReportScreen
import com.trasparenza.app.ui.saved.SavedProductsScreen
import com.trasparenza.app.ui.scanner.ScannerScreen
import com.trasparenza.app.ui.theme.TrasparenzaTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity - Entry point for the app
 * Uses Jetpack Compose for UI with bottom navigation
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrasparenzaTheme {
                TrasparenzaNavigation()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String) {
    object Scanner : Screen("scanner", "Scanner")
    object Receipt : Screen("receipt", "Scontrino")
    object Saved : Screen("saved", "Salvati")
}

// Routes that are NOT shown in bottom nav (detail screens)
object DetailRoute {
    const val SustainabilityReport = "sustainability_report"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrasparenzaNavigation() {
    val navController = rememberNavController()
    val bottomNavScreens = listOf(Screen.Scanner, Screen.Receipt, Screen.Saved)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavScreens.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = when (screen) {
                                    Screen.Scanner -> Icons.Filled.CameraAlt
                                    Screen.Receipt -> Icons.Filled.Receipt
                                    Screen.Saved -> Icons.Filled.List
                                },
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Scanner.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Scanner.route) {
                ScannerScreen()
            }
            composable(Screen.Receipt.route) {
                ReceiptScannerScreen(
                    onNavigateToReport = {
                        navController.navigate(DetailRoute.SustainabilityReport) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(DetailRoute.SustainabilityReport) {
                SustainabilityReportScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Saved.route) {
                SavedProductsScreen()
            }
        }
    }
}

