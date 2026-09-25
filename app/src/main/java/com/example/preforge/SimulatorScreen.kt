package com.example.preforge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.preforge.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun SimulatorScreen(
    questions: List<Question> = emptyList(),
    onNavigateToMenu: () -> Unit = {},
    answerViewModel: QuestionAnswerViewModel = viewModel()
) {
    if (questions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGreen)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No hay preguntas para mostrar",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Genera un examen o carga uno guardado desde la pantalla de exámenes.",
                fontSize = 14.sp,
                color = PrimaryGreen,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNavigateToMenu,
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Volver al menú", color = Color.White)
            }
        }
        return
    }

    // Detectar si el primer elemento es un mensaje de error de API o configuración
    val firstQuestionText = questions.firstOrNull()?.questionText ?: ""
    val isApiError = firstQuestionText.contains("Error", ignoreCase = true) ||
            firstQuestionText.contains("API", ignoreCase = true) ||
            firstQuestionText.contains("Key", ignoreCase = true) ||
            firstQuestionText.contains("Fallo", ignoreCase = true)

    if (isApiError) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGreen)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Logo en la parte superior izquierda para regresar al menú
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToMenu() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_preforge_bolt),
                    contentDescription = "Regresar al Menú",
                    colorFilter = ColorFilter.tint(DarkGreen),
                    modifier = Modifier.size(36.dp)
                )
            }

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFC62828)),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Atención",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = firstQuestionText,
                        fontSize = 15.sp,
                        color = PrimaryGreen,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = onNavigateToMenu,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Regresar al Menú Principal",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
        return
    }

    // --- ESTADOS DE NAVEGACIÓN Y PUNTUACIÓN ---
    var currentQuestionIndex by remember { mutableStateOf(0) }
    val answerState by answerViewModel.answerState.collectAsState()
    var answerHistory by remember { mutableStateOf<List<QuizAnswerAttempt>>(emptyList()) }
    var isQuizFinished by remember { mutableStateOf(false) }
    val score = answerHistory.count { it.isCorrect }
    val questionScrollState = rememberScrollState()

    LaunchedEffect(currentQuestionIndex) {
        questionScrollState.scrollTo(0)
        answerViewModel.reset()
    }

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
    val timeFormatted = String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)

    val totalQuestions = questions.size

    fun registerAnswer(questionIndex: Int, question: Question, answer: String) {
        if (answerViewModel.answerState.value.isSubmitted || answer.isBlank()) return

        val isCorrect = answerViewModel.submit(question, answer)
        answerHistory = answerHistory
            .filterNot { it.questionIndex == questionIndex }
            .plus(
                QuizAnswerAttempt(
                    questionIndex = questionIndex,
                    question = question,
                    selectedAnswer = answer,
                    isCorrect = isCorrect
                )
            )
    }

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
        QuizResultsContent(
            score = score,
            totalQuestions = totalQuestions,
            reviewAttempts = buildReviewAttempts(questions, answerHistory),
            onRestart = {
                currentQuestionIndex = 0
                answerViewModel.reset()
                answerHistory = emptyList()
                timeLeftInSeconds = 15 * 60
                isPaused = false
                isQuizFinished = false
            },
            onNavigateToMenu = onNavigateToMenu
        )
    } else {
        // --- PANTALLA DE PREGUNTAS Y RESPUESTAS ---
        val currentQuestion = questions[currentQuestionIndex]
        val progress = (currentQuestionIndex + 1).toFloat() / totalQuestions.toFloat()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGreen)
                .verticalScroll(questionScrollState)
                .padding(24.dp)
        ) {
            // 1. Encabezado: Pregunta actual y Cronómetro
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onNavigateToMenu() }
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_preforge_bolt),
                        contentDescription = "Regresar al Menú",
                        colorFilter = ColorFilter.tint(DarkGreen),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
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

            // 4. Renderizado según el tipo de pregunta
            when (currentQuestion.questionType) {
                QuestionType.MULTIPLE_CHOICE,
                QuestionType.TRUE_FALSE -> {
                    val opciones = if (
                        currentQuestion.questionType == QuestionType.TRUE_FALSE &&
                        currentQuestion.options.isEmpty()
                    ) {
                        listOf("Verdadero", "Falso")
                    } else {
                        currentQuestion.options
                    }

                    opciones.forEach { opcion ->
                        val isAnswered = answerState.isSubmitted
                        val isSelected = answerState.answer == opcion
                        val isCorrect = opcion.equals(
                            currentQuestion.correctAnswer,
                            ignoreCase = true
                        )
                        val containerColor = when {
                            !isAnswered -> Color.White
                            isSelected && isCorrect -> Color(0xFFE8F5E9)
                            isSelected -> Color(0xFFFFEBEE)
                            isCorrect -> Color(0xFFE8F5E9)
                            else -> Color.White
                        }
                        val borderColor = when {
                            !isAnswered -> LightGreen
                            isCorrect || isSelected -> {
                                if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                            }
                            else -> LightGreen
                        }

                        OutlinedButton(
                            onClick = {
                                registerAnswer(
                                    questionIndex = currentQuestionIndex,
                                    question = currentQuestion,
                                    answer = opcion
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .heightIn(min = 56.dp),
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
                                if (isAnswered && isCorrect) {
                                    Text("✓", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                } else if (isAnswered && isSelected) {
                                    Text("✗", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                QuestionType.OPEN,
                QuestionType.FILL_BLANKS -> {
                    val label = if (currentQuestion.questionType == QuestionType.FILL_BLANKS) {
                        "Completa los espacios"
                    } else {
                        "Escribe tu respuesta"
                    }
                    OutlinedTextField(
                        value = answerState.answer.orEmpty(),
                        onValueChange = answerViewModel::updateDraft,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !answerState.isSubmitted,
                        label = { Text(label) },
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                    answerState.validationMessage?.let { message ->
                        Text(
                            text = message,
                            color = Color(0xFFC62828),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    Button(
                        onClick = {
                            registerAnswer(
                                questionIndex = currentQuestionIndex,
                                question = currentQuestion,
                                answer = answerState.answer.orEmpty()
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .heightIn(min = 50.dp),
                        enabled = !answerState.isSubmitted && !answerState.answer.isNullOrBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Comprobar respuesta", color = Color.White)
                    }
                }
            }

            if (answerState.isSubmitted) {
                QuestionFeedback(
                    isCorrect = answerState.isCorrect == true,
                    correctAnswer = currentQuestion.correctAnswer,
                    explanation = currentQuestion.explanation,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Botones inferiores
            Button(
                onClick = {
                    if (currentQuestionIndex < totalQuestions - 1) {
                        currentQuestionIndex++
                    } else {
                        isQuizFinished = true
                    }
                },
                enabled = answerState.isSubmitted,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
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

internal data class QuizAnswerAttempt(
    val questionIndex: Int,
    val question: Question,
    val selectedAnswer: String?,
    val isCorrect: Boolean
)

internal fun buildReviewAttempts(
    questions: List<Question>,
    submittedAnswers: List<QuizAnswerAttempt>
): List<QuizAnswerAttempt> {
    val attemptsByQuestion = submittedAnswers.associateBy { it.questionIndex }
    return questions.mapIndexed { questionIndex, question ->
        attemptsByQuestion[questionIndex]?.copy(question = question)
            ?: QuizAnswerAttempt(
                questionIndex = questionIndex,
                question = question,
                selectedAnswer = null,
                isCorrect = false
            )
    }
}

@Composable
private fun QuestionFeedback(
    isCorrect: Boolean,
    correctAnswer: String,
    explanation: String,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val accentColor = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
    val feedbackTitle = if (isCorrect) "Respuesta correcta" else "Esta no era la respuesta"

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = feedbackTitle,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (!isCorrect) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "La respuesta correcta era: $correctAnswer",
                    color = DarkGreen,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = accentColor.copy(alpha = 0.22f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Qué conviene reforzar",
                        color = DarkGreen,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = explanation.ifBlank { guiaDeRepaso(correctAnswer) },
                        color = DarkGreen,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizResultsContent(
    score: Int,
    totalQuestions: Int,
    reviewAttempts: List<QuizAnswerAttempt>,
    onRestart: () -> Unit,
    onNavigateToMenu: () -> Unit
) {
    val incorrectAttempts = reviewAttempts.filterNot { it.isCorrect }
    val unansweredCount = incorrectAttempts.count { it.selectedAnswer == null }
    val percentage = if (totalQuestions > 0) {
        (score.toFloat() / totalQuestions.toFloat() * 100).toInt()
    } else {
        0
    }
    val scoreColor = if (score * 2 >= totalQuestions) {
        Color(0xFF2E7D32)
    } else {
        Color(0xFFC62828)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGreen)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateToMenu) {
                Image(
                    painter = painterResource(R.drawable.ic_preforge_bolt),
                    contentDescription = "Volver al menú",
                    colorFilter = ColorFilter.tint(DarkGreen),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Resultado del simulador",
                color = DarkGreen,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, LightGreen),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "¡Examen finalizado!",
                    color = DarkGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "$score / $totalQuestions",
                    color = scoreColor,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$percentage% de aciertos",
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (incorrectAttempts.isEmpty()) {
                        "Respondiste correctamente todas las preguntas."
                    } else {
                        "La retroalimentación de abajo muestra qué respuestas necesita repasar."
                    },
                    color = DarkGreen,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Retroalimentación inteligente",
            color = DarkGreen,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (incorrectAttempts.isEmpty()) {
            SuccessfulFeedbackCard()
        } else {
            FeedbackReviewSection(
                incorrectAttempts = incorrectAttempts,
                unansweredCount = unansweredCount
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Reiniciar simulador",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNavigateToMenu,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
            border = BorderStroke(1.5.dp, DarkGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Volver al menú",
                color = DarkGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SuccessfulFeedbackCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFE8F5E9),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Sin respuestas que repasar",
                    color = Color(0xFF1B5E20),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Has demostrado comprensión de todos los conceptos evaluados.",
                    color = DarkGreen,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun FeedbackReviewSection(
    incorrectAttempts: List<QuizAnswerAttempt>,
    unansweredCount: Int
) {
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    val allQuestionsUnanswered = unansweredCount == incorrectAttempts.size
    val reviewTitle = if (allQuestionsUnanswered) {
        "Preguntas pendientes de repaso"
    } else {
        "Repaso de respuestas incorrectas"
    }
    val summary = when {
        unansweredCount > 0 && incorrectAttempts.size > unansweredCount ->
            "Repasa estas ${incorrectAttempts.size} respuestas. Las preguntas sin responder se han incluido para que puedas recuperarlas."
        allQuestionsUnanswered && unansweredCount == 1 ->
            "Esta pregunta quedó sin responder. Lee la explicación y vuelve a intentarlo."
        allQuestionsUnanswered ->
            "Las $unansweredCount preguntas quedaron sin responder. Lee las explicaciones antes de volver a intentarlo."
        incorrectAttempts.size == 1 ->
            "Esta fue tu respuesta incorrecta. Contrasta la explicación con tus apuntes."
        else ->
            "Varios conceptos necesitan una segunda vuelta. Revisa estas explicaciones antes de repetir el examen."
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFFBF0),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFD6A84B))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF7A4E00),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reviewTitle,
                        color = Color(0xFF5C3A00),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summary,
                        color = DarkGreen,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (isExpanded) {
                            "Ocultar repaso"
                        } else {
                            "Mostrar repaso"
                        },
                        tint = Color(0xFF5C3A00)
                    )
                }
            }

            if (isExpanded) {
                incorrectAttempts.forEachIndexed { index, attempt ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = Color(0xFFE2C77D)
                        )
                    }
                    IncorrectAnswerReview(attempt = attempt)
                }
            }
        }
    }
}

@Composable
private fun IncorrectAnswerReview(attempt: QuizAnswerAttempt) {
    val question = attempt.question
    val wasUnanswered = attempt.selectedAnswer == null

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = if (wasUnanswered) Icons.Default.Warning else Icons.Default.Cancel,
                contentDescription = null,
                tint = Color(0xFFC62828),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pregunta ${attempt.questionIndex + 1}",
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = question.questionText,
                    color = DarkGreen,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (wasUnanswered) {
                "No respondiste esta pregunta."
            } else {
                "Esta no era la respuesta. Tu respuesta fue: ${attempt.selectedAnswer}"
            },
            color = Color(0xFFA61B1B),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "La respuesta correcta era: ${question.correctAnswer}",
            color = Color(0xFF1B5E20),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFFE2C77D))
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Qué repasar",
            color = Color(0xFF5C3A00),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = question.explanation.ifBlank { guiaDeRepaso(question.correctAnswer) },
            color = DarkGreen,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun guiaDeRepaso(correctAnswer: String): String =
    "Vuelve al apartado de apuntes sobre este tema y comprueba por qué «$correctAnswer» es la respuesta correcta. Después, intenta explicar el concepto con tus propias palabras."

@Preview(showBackground = true)
@Composable
fun SimulatorScreenPreview() {
    val answerViewModel = remember { QuestionAnswerViewModel() }
    PreForgeTheme {
        SimulatorScreen(
            answerViewModel = answerViewModel,
            questions = listOf(
                Question(
                    questionText = "¿Cuál es la función principal de la mitocondria?",
                    options = listOf("Síntesis de proteínas", "Producción de ATP", "Digestión celular", "Fotosíntesis"),
                    correctAnswer = "Producción de ATP",
                    explanation = "La mitocondria produce ATP mediante fosforilación oxidativa. Este proceso aporta energía para las funciones celulares."
                ),
                Question(
                    questionText = "¿Cuál es el órgano más grande del cuerpo humano?",
                    options = listOf("Hígado", "Cerebro", "Piel", "Pulmón"),
                    correctAnswer = "Piel",
                    explanation = "La piel forma la barrera exterior del cuerpo y representa gran parte de su peso total."
                )
            )
        )
    }
}