package com.example.preforge

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.preforge.ui.theme.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onLoginSuccess: (AppUser) -> Unit
) {
    val context = LocalContext.current
    val sessionStore = remember { SessionStore(context) }
    var showAuthSheet by remember { mutableStateOf(false) }
    var showGuestDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var guestName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isAuthLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    fun showAuthError(message: String) {
        isAuthLoading = false
        authError = message
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    val launchGoogleSignIn = rememberGoogleSignInLauncher(
        onSuccess = { firebaseUser ->
            isAuthLoading = false
            onLoginSuccess(
                AppUser(
                    id = firebaseUser.uid,
                    displayName = firebaseUser.displayName
                        ?: firebaseUser.email
                        ?: "Usuario de Google"
                )
            )
        },
        onError = { message -> showAuthError(message) }
    )

    fun submitEmailAuthentication() {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            showAuthError("Completa el correo y la contraseña")
            return
        }

        val firebaseAuth = try {
            Firebase.auth
        } catch (exception: Exception) {
            showAuthError("Firebase Auth no está inicializado")
            return
        }

        isAuthLoading = true
        authError = null
        val task = if (isRegisterMode) {
            firebaseAuth.createUserWithEmailAndPassword(normalizedEmail, password)
        } else {
            firebaseAuth.signInWithEmailAndPassword(normalizedEmail, password)
        }

        task.addOnCompleteListener { result ->
            isAuthLoading = false
            if (result.isSuccessful) {
                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser == null) {
                    showAuthError("Firebase no devolvió un usuario")
                } else {
                    showEmailDialog = false
                    onLoginSuccess(
                        AppUser(
                            id = firebaseUser.uid,
                            displayName = firebaseUser.displayName
                                ?: normalizedEmail.substringBefore("@")
                        )
                    )
                }
            } else {
                showAuthError(
                    result.exception?.localizedMessage
                        ?: "No se pudo completar la autenticación"
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGreen)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_preforge_bolt),
                contentDescription = "Logo de PreForge",
                colorFilter = ColorFilter.tint(DarkGreen),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "PreForge",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )
            Text(
                text = "AI EXAM ENGINE",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryGreen,
                letterSpacing = 2.sp
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "De tus apuntes a tu examen perfecto en segundos",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Carga tus resúmenes o PDFs y nuestra IA generará simuladores interactivos de alta fidelidad al instante.",
                fontSize = 14.sp,
                color = PrimaryGreen,
                textAlign = TextAlign.Center
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    isRegisterMode = false
                    authError = null
                    showAuthSheet = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Iniciar Sesión",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    isRegisterMode = true
                    authError = null
                    showAuthSheet = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Registrarse Gratis",
                    color = DarkGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showAuthSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAuthSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRegisterMode) "Crear una cuenta" else "Iniciar Sesión",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Elige cómo quieres entrar a tu cuenta",
                    fontSize = 13.sp,
                    color = PrimaryGreen,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = {
                        showAuthSheet = false
                        isAuthLoading = true
                        launchGoogleSignIn()
                    },
                    enabled = !isAuthLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LightGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Continuar con Google",
                        color = DarkGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        showAuthSheet = false
                        showEmailDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "Registrarse con Correo" else "Ingresar con Correo",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        showAuthSheet = false
                        showGuestDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Entrar como Invitado",
                        color = PrimaryGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showGuestDialog) {
        AlertDialog(
            onDismissRequest = { showGuestDialog = false },
            title = {
                Text(
                    text = "Acceso como Invitado",
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )
            },
            text = {
                Column {
                    Text(
                        text = "Escribe tu nombre para asociar tus exámenes a este usuario local:",
                        fontSize = 14.sp,
                        color = PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = guestName,
                        onValueChange = { guestName = it },
                        label = { Text("Tu Nombre") },
                        singleLine = true,
                        enabled = !isAuthLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkGreen,
                            unfocusedBorderColor = LightGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGuestDialog = false
                        onLoginSuccess(sessionStore.getOrCreateGuest(guestName))
                    },
                    enabled = !isAuthLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Ingresar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGuestDialog = false }) {
                    Text("Cancelar", color = PrimaryGreen)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = {
                Text(
                    text = if (isRegisterMode) "Registro de Usuario" else "Iniciar Sesión",
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico") },
                        singleLine = true,
                        enabled = !isAuthLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkGreen,
                            unfocusedBorderColor = LightGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        enabled = !isAuthLoading,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkGreen,
                            unfocusedBorderColor = LightGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (authError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = authError.orEmpty(),
                            color = Color(0xFFC62828),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { submitEmailAuthentication() },
                    enabled = !isAuthLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (isAuthLoading) "Procesando..." else if (isRegisterMode) "Crear Cuenta" else "Entrar",
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEmailDialog = false },
                    enabled = !isAuthLoading
                ) {
                    Text("Cancelar", color = PrimaryGreen)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    PreForgeTheme {
        WelcomeScreen(onLoginSuccess = {})
    }
}
