package com.example.preforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.preforge.ui.theme.PreForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PreForgeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(
                onLoginSuccess = { userName ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("user_name", userName)
                    navController.navigate("main") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            val userName = navController.previousBackStackEntry?.savedStateHandle?.get<String>("user_name") ?: "Estudiante"
            MainScreen(
                userName = userName,
                onNavigateToSimulator = { questions ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("questions_list", ArrayList(questions))
                    navController.navigate("simulator")
                },
                onLogout = {
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            val userName = navController.previousBackStackEntry?.savedStateHandle?.get<String>("user_name") ?: "Estudiante"
            MainScreen(
                userName = userName,
                onNavigateToSimulator = { questions ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("questions_list", ArrayList(questions))
                    navController.navigate("simulator")
                },
                onLogout = {
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("simulator") {
            val questions = navController.previousBackStackEntry?.savedStateHandle?.get<ArrayList<Question>>("questions_list") ?: emptyList<Question>()
            SimulatorScreen(questions = questions)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppNavigationPreview() {
    PreForgeTheme {
        WelcomeScreen(onLoginSuccess = {})
    }
}