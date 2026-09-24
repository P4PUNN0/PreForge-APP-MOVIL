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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.preforge.ui.theme.PreForgeTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

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
    val context = LocalContext.current
    val sessionStore = remember { SessionStore(context) }
    var currentUser by remember { mutableStateOf<AppUser?>(null) }

    LaunchedEffect(Unit) {
        val firebaseUser = try {
            Firebase.auth.currentUser
        } catch (exception: Exception) {
            null
        }
        val restoredUser = firebaseUser?.let { user ->
            AppUser(
                id = user.uid,
                displayName = user.displayName ?: user.email ?: "Estudiante"
            )
        } ?: sessionStore.loadGuest()

        if (restoredUser != null) {
            currentUser = restoredUser
            navController.navigate("main") {
                popUpTo("welcome") { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(
                onLoginSuccess = { user ->
                    currentUser = user
                    navController.navigate("main") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            val user = currentUser ?: AppUser(
                id = "local-user",
                displayName = "Estudiante"
            )
            MainScreen(
                userId = user.id,
                userName = user.displayName,
                onNavigateToSimulator = { questions ->
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "questions_list",
                        ArrayList(questions)
                    )
                    navController.navigate("simulator")
                },
                onLogout = {
                    if (!user.isGuest) {
                        try {
                            Firebase.auth.signOut()
                        } catch (exception: Exception) {
                            // Firebase may be unavailable in local-only mode.
                        }
                    }
                    sessionStore.clear()
                    currentUser = null
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            val user = currentUser ?: AppUser(
                id = "local-user",
                displayName = "Estudiante"
            )
            MainScreen(
                userId = user.id,
                userName = user.displayName,
                onNavigateToSimulator = { questions ->
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "questions_list",
                        ArrayList(questions)
                    )
                    navController.navigate("simulator")
                },
                onLogout = {
                    if (!user.isGuest) {
                        try {
                            Firebase.auth.signOut()
                        } catch (exception: Exception) {
                            // Firebase may be unavailable in local-only mode.
                        }
                    }
                    sessionStore.clear()
                    currentUser = null
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("simulator") {
            val questions = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<ArrayList<Question>>("questions_list")
                ?: emptyList()
            SimulatorScreen(
                questions = questions,
                onNavigateToMenu = {
                    navController.popBackStack()
                }
            )
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
