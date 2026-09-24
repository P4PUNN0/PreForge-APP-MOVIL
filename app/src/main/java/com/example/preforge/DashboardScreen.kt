package com.example.preforge

import android.app.Activity
import android.content.Intent // Importación crucial agregada
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue // Importación crucial agregada para "by remember"
import androidx.compose.runtime.setValue // Importación crucial agregada para "by remember"
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.preforge.data.local.AppDatabase
import com.example.preforge.data.local.toEntity
import com.example.preforge.data.local.toQuestion
import com.example.preforge.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

// --- LAUNCHER DE AUTENTICACIÓN GOOGLE + FIREBASE ---
@Composable
fun rememberGoogleSignInLauncher(
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    if (isPreview) return {}
    val auth = remember {
        try { Firebase.auth } catch (e: Exception) { null }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                auth?.signInWithCredential(credential)
                    ?.addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            val user = auth.currentUser
                            onSuccess(user?.displayName ?: user?.email ?: "Estudiante")
                        } else {
                            onError(authTask.exception?.localizedMessage ?: "Error al autenticar en Firebase")
                        }
                    }
            } catch (e: ApiException) {
                onError("Google Error (${e.statusCode}): ${e.message}")
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al seleccionar la cuenta")
            }
        } else {
            onError("Inicio de sesión cancelado")
        }
    }

    return {
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId == 0) {
                Toast.makeText(context, "Realiza 'Sync Project with Gradle Files' en Android Studio", Toast.LENGTH_LONG).show()
            } else {
                val webClientId = context.getString(resId)
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()

                val googleSignInClient = GoogleSignIn.getClient(context, gso)

                googleSignInClient.signOut().addOnCompleteListener {
                    // Ahora launcher.launch reconocerá correctamente el Intent
                    launcher.launch(googleSignInClient.signInIntent)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al lanzar Google Sign-In: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}

// --- PANTALLA PRINCIPAL ---
@Composable
fun DashboardScreen(
    userName: String = "Estudiante",
    onNavigateToSimulator: (List<Question>) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isPreview = LocalInspectionMode.current
    val auth = remember {
        if (!isPreview) {
            try { Firebase.auth } catch (e: Exception) { null }
        } else null
    }

    var currentUserName by remember {
        mutableStateOf(auth?.currentUser?.displayName ?: userName)
    }

    val launchGoogleSignIn = rememberGoogleSignInLauncher(
        onSuccess = { name ->
            currentUserName = name
            Toast.makeText(context, "¡Sesión iniciada como $name!", Toast.LENGTH_SHORT).show()
        },
        onError = { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    )

    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                selectedFileUri = it
                val cursor = context.contentResolver.query(it, null, null, null, null)
                val name = cursor?.use { c ->
                    val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && c.moveToFirst()) c.getString(nameIndex) else null
                } ?: it.path
                selectedFileName = name
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGreen)
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { launchGoogleSignIn() }
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Perfil",
                    tint = DarkGreen,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "¡Bienvenido/a!", fontSize = 12.sp, color = PrimaryGreen)
                    Text(
                        text = currentUserName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                }
                TextButton(onClick = { launchGoogleSignIn() }) {
                    Text(
                        text = if (auth?.currentUser != null) "Cambiar" else "Google Sign-In",
                        color = DarkGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Sube tus apuntes",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )
            Text(
                text = "Sube archivos, fotos de tus libretas o texto y déjalos listos para el examen.",
                fontSize = 14.sp,
                color = PrimaryGreen
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(2.dp, LightGreen, RoundedCornerShape(16.dp))
                    .clickable {
                        filePickerLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "text/plain"
                            )
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (selectedFileName != null) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Subir",
                            tint = DarkGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Archivo seleccionado:",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedFileName ?: "",
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Subir",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Arrastra o selecciona tus archivos",
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen
                        )
                        Text(
                            text = "Admite PDF, DOCX, TXT hasta 20 MB",
                            fontSize = 12.sp,
                            color = PrimaryGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    isLoading = true
                    coroutineScope.launch {
                        var fileTextContent = ""
                        selectedFileUri?.let { uri ->
                            try {
                                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                                        fileTextContent = reader.readText()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        if (fileTextContent.isBlank()) {
                            fileTextContent = "Contenido simulado de apuntes de estudio."
                        }

                        val questions = AiEngine.generateQuestions(fileTextContent)
                        try {
                            val db = AppDatabase.getDatabase(context)
                            db.questionDao().insertAll(questions.map { it.toEntity() })
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        isLoading = false
                        onNavigateToSimulator(questions)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                Text(text = "Generar Simulador", color = Color.White, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            val savedEntities = db.questionDao().getAllQuestionsList()
                            val savedQuestions = savedEntities.map { it.toQuestion() }
                            isLoading = false
                            if (savedQuestions.isNotEmpty()) {
                                onNavigateToSimulator(savedQuestions)
                            } else {
                                Toast.makeText(context, "No hay preguntas guardadas en Room", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            Toast.makeText(context, "Error al cargar desde Room: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Cargar Examen Guardado (Room)", color = DarkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DarkGreen)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    PreForgeTheme {
        DashboardScreen(userName = "Estudiante")
    }
}