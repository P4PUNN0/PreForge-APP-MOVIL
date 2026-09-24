package com.example.preforge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.preforge.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun SimulatorScreen(questions: List<Question> = emptyList()) {
    if (questions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = DarkGreen
            )
        }
        return
    }

    // --- ESTADOS DE NAVEGACIÓN Y PUNTUACIÓN ---
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var score by remember { mutableStateOf(0) }
    var isQuizFinished by remember { mutableStateOf(false) }

    // --- ESTADOS PARA TEMPORIZADOR Y PAUSA ---
    var timeLeftInSeconds by remember { mutableStateOf(15 * 60) } // 15 minutos en segundos
    var isPaused by remember { mutableStateOf(false) }

    // Corutina para descontar el tiempo segundo a segundo
    LaunchedEffect(key1 = timeLeftInSeconds, key2 = isPaused, key3 = isQuizFinished) {
        if (!isPaused && !isQuizFinished && timeLeftInSeconds > 0) {
            delay(1000L)
            timeLeftInSeconds--
        } else if (timeLeftInSeconds == 0 && !isQuizFinished) {
            isQuizFinished = true // Finaliza la prueba automáticamente si se acaba el tiempo
        }
    }

    val minutes = timeLeftInSeconds / 60
    val seconds = timeLeftInSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    val totalQuestions = questions.size

    // --- DIÁLOGO DE PAUSA ---
    if (isPaused) {
        AlertDialog(
            onDismissRequest = { isPaused = false },
            title = {
                Text(
                    text = "Simulador Pausado",
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )
            },
            text = {
                Text(
                    text = "El cronómetro está detenido. ¿Deseas reanudar la prueba o darla por terminada?",
                    color = PrimaryGreen
                )
            },
            confirmButton = {
                Button(
                    onClick = { isPaused = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Reanudar Examen", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isPaused = false
                        isQuizFinished = true
                    }
                ) {
                    Text("Terminar Examen", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // --- PANTALLA DE RESULTADOS / PUNTUACIÓN ---
    if (isQuizFinished) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGreen)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, LightGreen),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "¡Examen Finalizado!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Puntuación Obtenida",
                        fontSize = 16.sp,
                        color = PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$score / $totalQuestions",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (score >= totalQuestions / 2) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )

                    val porcentaje = ((score.toFloat() / totalQuestions.toFloat()) * 100).toInt()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$porcentaje% de aciertos",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            currentQuestionIndex = 0
                            selectedOption = null
                            score = 0
                            timeLeftInSeconds = 15 * 60
                            isPaused = false
                            isQuizFinished = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Reiniciar Simulador",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    } else {
        // --- PANTALLA DE PREGUNTAS Y RESPUESTAS ---
        val currentQuestion = questions[currentQuestionIndex]
        val progress = (currentQuestionIndex + 1).toFloat() / totalQuestions.toFloat()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGreen)
                .padding(24.dp)
        ) {
            // 1. Encabezado: Pregunta actual y Cronómetro
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = "Pregunta", tint = DarkGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pregunta ${currentQuestionIndex + 1}/$totalQuestions",
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                }

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.border(1.dp, LightGreen, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Tiempo",
                            tint = if (timeLeftInSeconds <= 60) Color(0xFFC62828) else DarkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timeFormatted,
                            fontWeight = FontWeight.Bold,
                            color = if (timeLeftInSeconds <= 60) Color(0xFFC62828) else DarkGreen,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Barra de progreso
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Progreso Simulador", fontSize = 12.sp, color = PrimaryGreen)
                Text(
                    text = "${((progress) * 100).toInt()}% completado",
                    fontSize = 12.sp,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = DarkGreen,
                trackColor = LightGreen,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Etiqueta de la materia y Pregunta
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, LightGreen, RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = "EXAMEN PREFORGE",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = currentQuestion.questionText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Opciones de respuesta dinámicas
            currentQuestion.options.forEach { opcion ->
                val isAnswered = selectedOption != null
                val isSelected = selectedOption == opcion
                val isCorrect = opcion == currentQuestion.correctAnswer

                // Colores dinámicos según el estado de la respuesta
                val containerColor = when {
                    !isAnswered -> Color.White
                    isSelected && isCorrect -> Color(0xFFE8F5E9)   // Fondo verde si acertó
                    isSelected && !isCorrect -> Color(0xFFFFEBEE)  // Fondo rojo si falló
                    !isSelected && isCorrect -> Color(0xFFE8F5E9)  // Muestra la correcta en verde
                    else -> Color.White
                }

                val borderColor = when {
                    !isAnswered -> LightGreen
                    isSelected && isCorrect -> Color(0xFF2E7D32)
                    isSelected && !isCorrect -> Color(0xFFC62828)
                    !isSelected && isCorrect -> Color(0xFF2E7D32)
                    else -> LightGreen
                }

                OutlinedButton(
                    onClick = {
                        // Evita cambiar la respuesta una vez seleccionada
                        if (selectedOption == null) {
                            selectedOption = opcion
                            if (isCorrect) {
                                score++
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = containerColor),
                    border = BorderStroke(1.5.dp, borderColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = opcion,
                            color = DarkGreen,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                        // Indicador de acierto / error
                        if (isAnswered) {
                            if (isCorrect) {
                                Text("✓", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            } else if (isSelected) {
                                Text("✗", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 5. Botones inferiores
            Button(
                onClick = {
                    if (currentQuestionIndex < totalQuestions - 1) {
                        currentQuestionIndex++
                        selectedOption = null // Restablece la selección para la siguiente pregunta
                    } else {
                        isQuizFinished = true // Abre la pantalla de puntuación final
                    }
                },
                enabled = selectedOption != null, // Requiere seleccionar una opción para avanzar
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (currentQuestionIndex == totalQuestions - 1) "Finalizar Examen" else "Siguiente Pregunta",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { isPaused = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Pausar Simulador", color = PrimaryGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SimulatorScreenPreview() {
    PreForgeTheme {
        SimulatorScreen(
            questions = listOf(
                Question(
                    questionText = "¿Cuál es la función principal de la mitocondria?",
                    options = listOf("Síntesis de proteínas", "Producción de ATP", "Digestión celular", "Fotosíntesis"),
                    correctAnswer = "Producción de ATP"
                ),
                Question(
                    questionText = "¿Cuál es el órgano más grande del cuerpo humano?",
                    options = listOf("Hígado", "Cerebro", "Piel", "Pulmón"),
                    correctAnswer = "Piel"
                )
            )
        )
    }
}