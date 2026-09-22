package com.example.preforge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import com.example.preforge.data.local.AppDatabase
import com.example.preforge.data.local.QuestionEntity
import com.example.preforge.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Estados de la interfaz
    var listaPreguntas by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }
    var indiceActual by remember { mutableStateOf(0) }
    var puntuacion by remember { mutableStateOf(0) }
    var simuladorTerminado by remember { mutableStateOf(false) }
    var opcionSeleccionada by remember { mutableStateOf<String?>(null) }
    var mostrarFeedback by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(true) }

    // Inicializar Room y cargar las preguntas al abrir la pantalla
    LaunchedEffect(Unit) {
        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "preforge_database"
        ).build()

        val preguntas = withContext(Dispatchers.IO) {
            db.questionDao().getAllQuestions()
        }

        listaPreguntas = preguntas.shuffled() // Orden aleatorio para el examen
        cargando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Simulador de Examen", color = DarkGreen, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = DarkGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundGreen)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGreen)
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (cargando) {
                Spacer(modifier = Modifier.height(100.dp))
                CircularProgressIndicator(color = DarkGreen)
                Text("Cargando preguntas de la base de datos...", modifier = Modifier.padding(top = 16.dp))
                return@Column
            }

            if (listaPreguntas.isEmpty()) {
                Spacer(modifier = Modifier.height(100.dp))
                Text(
                    text = "No hay preguntas guardadas.\nVe al Dashboard y sube un apunte primero.",
                    color = DarkGreen,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
                return@Column
            }

            if (simuladorTerminado) {
                // PANTALLA DE RESULTADOS
                Spacer(modifier = Modifier.height(50.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("¡Examen Terminado!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tu puntuación: $puntuacion / ${listaPreguntas.size}",
                            fontSize = 20.sp,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen)
                ) {
                    Text("Volver al Dashboard", color = Color.White, fontSize = 16.sp)
                }
                return@Column
            }

            // PANTALLA DEL CUESTIONARIO (Pregunta Activa)
            val preguntaActual = listaPreguntas[indiceActual]

            Text(
                text = "Pregunta ${indiceActual + 1} de ${listaPreguntas.size}",
                fontSize = 14.sp,
                color = PrimaryGreen,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = preguntaActual.topic.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Text(
                    text = preguntaActual.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkGreen,
                    modifier = Modifier.padding(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botones de opciones
            val opciones = listOf(
                "A" to preguntaActual.optionA,
                "B" to preguntaActual.optionB,
                "C" to preguntaActual.optionC,
                "D" to preguntaActual.optionD
            )

            opciones.forEach { (letra, texto) ->
                val esSeleccionada = opcionSeleccionada == letra
                val esCorrecta = letra == preguntaActual.correctAnswer

                // Lógica de colores para el feedback visual
                val backgroundColor = when {
                    !mostrarFeedback -> if (esSeleccionada) PrimaryGreen.copy(alpha = 0.2f) else Color.Transparent
                    esCorrecta -> Color(0xFFD4EDDA) // Verde claro para la correcta
                    esSeleccionada && !esCorrecta -> Color(0xFFF8D7DA) // Rojo claro para la incorrecta
                    else -> Color.Transparent
                }

                val borderColor = when {
                    !mostrarFeedback -> if (esSeleccionada) DarkGreen else Color.LightGray
                    esCorrecta -> Color(0xFF28A745)
                    esSeleccionada && !esCorrecta -> Color(0xFFDC3545)
                    else -> Color.LightGray
                }

                OutlinedButton(
                    onClick = {
                        if (!mostrarFeedback) opcionSeleccionada = letra
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).heightIn(min = 56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = backgroundColor),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$letra.",
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = texto,
                            color = DarkGreen,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón de acción inferior (Calificar o Siguiente)
            Button(
                onClick = {
                    if (!mostrarFeedback) {
                        // Calificar
                        if (opcionSeleccionada == preguntaActual.correctAnswer) {
                            puntuacion++
                        }
                        mostrarFeedback = true
                    } else {
                        // Pasar a la siguiente
                        if (indiceActual < listaPreguntas.size - 1) {
                            indiceActual++
                            opcionSeleccionada = null
                            mostrarFeedback = false
                        } else {
                            simuladorTerminado = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = opcionSeleccionada != null,
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen)
            ) {
                Text(
                    text = if (!mostrarFeedback) "Confirmar Respuesta" else if (indiceActual < listaPreguntas.size - 1) "Siguiente Pregunta" else "Finalizar Examen",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}