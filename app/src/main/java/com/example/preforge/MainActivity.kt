package com.example.preforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.preforge.ui.theme.PreForgeTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PreForgeTheme { // Usa el nombre real de tu tema
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        // AQUÍ ARRANCAMOS EL MAPA DE RUTAS
                        AppNavigation()
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}



@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(onNavigateToDashboard = { navController.navigate("dashboard") })
        }
        composable("dashboard") {
            DashboardScreen(onNavigateToSimulator = { navController.navigate("simulator") })
        }
        composable("simulator") { // <- NUEVA PARADA
            // Le pasamos la función para que la flecha hacia atrás funcione
            SimulatorScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}

