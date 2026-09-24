package com.example.preforge

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.preforge.data.local.AppDatabase
import com.example.preforge.data.local.ExamEntity
import com.example.preforge.data.local.toQuestion
import com.example.preforge.data.local.toEntity
import com.example.preforge.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    userId: String = "local-user",
    userName: String = "Estudiante",
    onNavigateToSimulator: (List<Question>) -> Unit = {}
) {
    val contexto = LocalContext.current
    val ambitoCorrutina = rememberCoroutineScope()
    var nombreArchivoSeleccionado by remember { mutableStateOf<String?>(null) }
    var uriArchivoSeleccionado by remember { mutableStateOf<Uri?>(null) }
    var cantidadPreguntas by remember { mutableIntStateOf(5) }
    var cargando by remember { mutableStateOf(false) }

    val selectorArchivo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { archivoUri ->
            uriArchivoSeleccionado = archivoUri
            val cursor = contexto.contentResolver.query(archivoUri, null, null, null, null)
            val nombre = cursor?.use { valorCursor ->
                val indiceNombre = valorCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (indiceNombre != -1 && valorCursor.moveToFirst()) {
                    valorCursor.getString(indiceNombre)
                } else {
                    null
                }
            } ?: archivoUri.lastPathSegment
            nombreArchivoSeleccionado = nombre ?: "Apuntes"
        }
    }

    fun crearTituloExamen(): String {
        val nombreBase = nombreArchivoSeleccionado
            ?.substringBeforeLast('.')
            ?.trim()
            ?.takeIf { valor -> valor.isNotBlank() }
            ?: "Examen"
        val marcaTiempo = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date())
        return "$nombreBase - $marcaTiempo"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGreen)
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Perfil",
                    tint = DarkGreen,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Sesión activa", fontSize = 12.sp, color = PrimaryGreen)
                    Text(
                        text = userName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                    Text(
                        text = "ID: $userId",
                        fontSize = 10.sp,
                        color = PrimaryGreen,
                        maxLines = 1
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
                text = "Cada simulador se guardará automáticamente en tu cuenta local.",
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
                        selectorArchivo.launch(
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
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Subir apuntes",
                        tint = if (nombreArchivoSeleccionado != null) DarkGreen else PrimaryGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (nombreArchivoSeleccionado != null) {
                        Text(
                            text = "Archivo seleccionado:",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = nombreArchivoSeleccionado.orEmpty(),
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        Text(
                            text = "Arrastra o selecciona tus archivos",
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen
                        )
                        Text(
                            text = "Admite PDF con OCR, DOCX y TXT hasta 20 MB",
                            fontSize = 12.sp,
                            color = PrimaryGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Número de preguntas:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )
            Spacer(modifier = Modifier.height(8.dp))

            val opcionesCantidad = listOf(5, 10, 15, 20)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                opcionesCantidad.forEach { cantidad ->
                    val estaSeleccionada = cantidadPreguntas == cantidad
                    OutlinedButton(
                        onClick = { cantidadPreguntas = cantidad },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (estaSeleccionada) DarkGreen else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (estaSeleccionada) DarkGreen else LightGreen
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "$cantidad",
                            color = if (estaSeleccionada) Color.White else DarkGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    cargando = true
                    ambitoCorrutina.launch {
                        try {
                            val uriArchivo = uriArchivoSeleccionado
                                ?: throw DocumentExtractionException(
                                    "Selecciona un archivo antes de generar el simulador"
                                )
                            val textoArchivo = withContext(Dispatchers.IO) {
                                DocumentTextExtractor.extract(
                                    contexto = contexto,
                                    uri = uriArchivo,
                                    nombreArchivo = nombreArchivoSeleccionado
                                )
                            }

                            val preguntas = withContext(Dispatchers.IO) {
                                AiEngine.generateQuestions(textoArchivo, cantidadPreguntas)
                            }
                            val examen = ExamEntity(
                                title = crearTituloExamen(),
                                userId = userId,
                                ownerName = userName,
                                sourceFileName = nombreArchivoSeleccionado,
                                questionCount = preguntas.size
                            )

                            withContext(Dispatchers.IO) {
                                AppDatabase.getDatabase(contexto)
                                    .examDao()
                                    .insertExamWithQuestions(
                                        exam = examen,
                                        questions = preguntas.map { pregunta -> pregunta.toEntity() }
                                    )
                            }

                            cargando = false
                            Toast.makeText(
                                contexto,
                                "Examen guardado automáticamente en Room",
                                Toast.LENGTH_SHORT
                            ).show()
                            onNavigateToSimulator(preguntas)
                        } catch (excepcion: CancellationException) {
                            cargando = false
                            throw excepcion
                        } catch (excepcion: Exception) {
                            cargando = false
                            val mensajeError = when (excepcion) {
                                is DocumentExtractionException -> excepcion.message
                                is QuestionGenerationException -> excepcion.message
                                else -> null
                            } ?: "Ocurrió un error inesperado. Inténtalo de nuevo"
                            Toast.makeText(
                                contexto,
                                "No se pudo generar el simulador: $mensajeError",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                shape = RoundedCornerShape(12.dp),
                enabled = !cargando && uriArchivoSeleccionado != null
            ) {
                Text(
                    text = if (cargando) "Procesando y generando..." else "Generar Simulador",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    cargando = true
                    ambitoCorrutina.launch {
                        try {
                            val ultimoExamen = withContext(Dispatchers.IO) {
                                AppDatabase.getDatabase(contexto)
                                    .examDao()
                                    .getLatestExamForUser(userId)
                            }
                            cargando = false
                            if (ultimoExamen != null && ultimoExamen.questions.isNotEmpty()) {
                                onNavigateToSimulator(
                                    ultimoExamen.questions.map { pregunta -> pregunta.toQuestion() }
                                )
                            } else {
                                Toast.makeText(
                                    contexto,
                                    "No hay exámenes guardados para este usuario",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (excepcion: Exception) {
                            cargando = false
                            Toast.makeText(
                                contexto,
                                "Error al cargar desde Room: ${excepcion.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkGreen),
                shape = RoundedCornerShape(12.dp),
                enabled = !cargando
            ) {
                Text(
                    text = "Cargar Último Examen (Room)",
                    color = DarkGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (cargando) {
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
        DashboardScreen(userId = "preview-user", userName = "Estudiante")
    }
}
