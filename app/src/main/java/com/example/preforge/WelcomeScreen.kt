package com.example.preforge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.preforge.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onLoginSuccess: (userName: String) -> Unit
) {
    var showAuthSheet by remember { mutableStateOf(false) }
    var showGuestDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }

    var guestName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGreen)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Logotipo y Título principal
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Logo",
                tint = DarkGreen,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "PrepForge",
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

        // Texto descriptivo
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

        // Botones principales
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    isRegisterMode = false
                    showAuthSheet = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Iniciar Sesión", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    isRegisterMode = true
                    showAuthSheet = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Registrarse Gratis", color = DarkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // --- BOTTOM SHEET DE AUTENTICACIÓN ---
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
                    text = "Selecciona tu método preferido para ingresar",
                    fontSize = 13.sp,
                    color = PrimaryGreen,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Opción 1: Google
                OutlinedButton(
                    onClick = {
                        showAuthSheet = false
                        onLoginSuccess("Usuario de Google")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LightGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continuar con Google", color = DarkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Opción 2: Correo y Contraseña
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

                // Opción 3: Modo Invitado
                TextButton(
                    onClick = {
                        showAuthSheet = false
                        showGuestDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Entrar como Invitado", color = PrimaryGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- DIÁLOGO MODO INVITADO (SOLICITA NOMBRE) ---
    if (showGuestDialog) {
        AlertDialog(
            onDismissRequest = { showGuestDialog = false },
            title = {
                Text(text = "Acceso como Invitado", fontWeight = FontWeight.Bold, color = DarkGreen)
            },
            text = {
                Column {
                    Text(
                        text = "Por favor ingresa tu nombre para personalizar tu experiencia:",
                        fontSize = 14.sp,
                        color = PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = guestName,
                        onValueChange = { guestName = it },
                        label = { Text("Tu Nombre") },
                        singleLine = true,
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
                        val finalName = guestName.trim().ifEmpty { "Invitado" }
                        onLoginSuccess(finalName)
                    },
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

    // --- DIÁLOGO DE CORREO / CONTRASEÑA ---
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
                        visualTransformation = PasswordVisualTransformation(),
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
                        if (email.isNotBlank() && password.isNotBlank()) {
                            showEmailDialog = false
                            val userNameFromEmail = email.substringBefore("@")
                            onLoginSuccess(userNameFromEmail)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isRegisterMode) "Crear Cuenta" else "Entrar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailDialog = false }) {
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