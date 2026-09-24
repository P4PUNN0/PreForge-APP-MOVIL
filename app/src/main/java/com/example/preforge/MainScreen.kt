package com.example.preforge

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.preforge.data.local.ExamWithQuestions
import com.example.preforge.data.local.toQuestion
import com.example.preforge.ui.theme.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun MainScreen(
    userId: String = "local-user",
    userName: String = "Estudiante",
    onNavigateToSimulator: (List<Question>) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var currentTab by remember { mutableStateOf("home") }

    val items = listOf(
        BottomNavItem("home", "Inicio", Icons.Default.Home),
        BottomNavItem("upload", "Subir", Icons.Default.CloudUpload),
        BottomNavItem("exams", "Exámenes", Icons.Default.Assignment),
        BottomNavItem("profile", "Perfil", Icons.Default.Person)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                items.forEach { item ->
                    val isSelected = currentTab == item.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isSelected) DarkGreen else PrimaryGreen
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) DarkGreen else PrimaryGreen
                            )
                        },
                        selected = isSelected,
                        onClick = { currentTab = item.route },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = BackgroundGreen
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "home" -> HomeScreen(
                    userId = userId,
                    userName = userName,
                    onNavigateToUpload = { currentTab = "upload" },
                    onNavigateToExams = { currentTab = "exams" }
                )
                "upload" -> DashboardScreen(
                    userId = userId,
                    userName = userName,
                    onNavigateToSimulator = onNavigateToSimulator
                )
                "exams" -> ExamsScreen(
                    userId = userId,
                    onNavigateToSimulator = onNavigateToSimulator
                )
                "profile" -> ProfileScreen(
                    userId = userId,
                    userName = userName,
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    userId: String,
    userName: String,
    onNavigateToUpload: () -> Unit,
    onNavigateToExams: () -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val examCountFlow = remember(userId, isPreview) {
        if (!isPreview) {
            try {
                AppDatabase.getDatabase(context)
                    .examDao()
                    .observeExamCount(userId)
            } catch (exception: Exception) {
                flowOf(0)
            }
        } else {
            flowOf(1)
        }
    }
    val examCount by examCountFlow.collectAsState(initial = 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGreen)
            .padding(24.dp)
    ) {
        Surface(
            color = DarkGreen,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "¡Hola, $userName!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tus apuntes, tus exámenes y tu progreso en un solo lugar.",
                    fontSize = 13.sp,
                    color = LightGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, LightGreen)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mis exámenes guardados",
                        fontSize = 12.sp,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$examCount en Room",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                }
                Surface(
                    color = BackgroundGreen,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("⚡", fontSize = 20.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Acciones rápidas",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToUpload() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, LightGreen)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Subir apuntes",
                    tint = DarkGreen,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Subir archivo y generar IA",
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Se guardará automáticamente en tu usuario",
                        fontSize = 12.sp,
                        color = PrimaryGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToExams() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, LightGreen)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = "Exámenes",
                    tint = DarkGreen,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ver mis exámenes",
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Abre cualquier examen guardado en tu dispositivo",
                        fontSize = 12.sp,
                        color = PrimaryGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun ExamsScreen(
    userId: String,
    onNavigateToSimulator: (List<Question>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isPreview = LocalInspectionMode.current
    val examsFlow = remember(userId, isPreview) {
        if (!isPreview) {
            try {
                AppDatabase.getDatabase(context)
                    .examDao()
                    .observeExamsForUser(userId)
            } catch (exception: Exception) {
                flowOf(emptyList<ExamWithQuestions>())
            }
        } else {
            flowOf(
                listOf(
                    ExamWithQuestions(
                        exam = com.example.preforge.data.local.ExamEntity(
                            title = "Biología celular",
                            userId = userId,
                            ownerName = "Estudiante",
                            questionCount = 2
                        ),
                        questions = listOf(
                            com.example.preforge.data.local.QuestionEntity(
                                questionText = "¿Cuál es la función de la mitocondria?",
                                options = listOf("Producir ATP", "Almacenar ADN"),
                                correctAnswer = "Producir ATP",
                                examId = 1
                            )
                        )
                    )
                )
            )
        }
    }
    val exams by examsFlow.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGreen)
            .padding(24.dp)
    ) {
        Text(
            text = "Mis exámenes",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen
        )
        Text(
            text = "Cada examen pertenece a tu usuario y queda guardado en Room.",
            fontSize = 13.sp,
            color = PrimaryGreen
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (exams.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(exams, key = { it.exam.id }) { examWithQuestions ->
                    ExamCard(
                        examWithQuestions = examWithQuestions,
                        onStart = {
                            onNavigateToSimulator(
                                examWithQuestions.questions.map { it.toQuestion() }
                            )
                        },
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    AppDatabase.getDatabase(context)
                                        .examDao()
                                        .deleteExam(examWithQuestions.exam.id, userId)
                                    Toast.makeText(
                                        context,
                                        "Examen eliminado",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (exception: Exception) {
                                    Toast.makeText(
                                        context,
                                        "No se pudo eliminar el examen",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }
                item {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    AppDatabase.getDatabase(context)
                                        .examDao()
                                        .deleteAllForUser(userId)
                                    Toast.makeText(
                                        context,
                                        "Tus exámenes fueron eliminados",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (exception: Exception) {
                                    Toast.makeText(
                                        context,
                                        "No se pudieron eliminar los exámenes",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        border = BorderStroke(1.dp, Color(0xFFC62828)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFC62828)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Borrar todos mis exámenes", color = Color(0xFFC62828))
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aún no tienes exámenes guardados.\n\nGenera uno desde la sección Subir.",
                    textAlign = TextAlign.Center,
                    color = PrimaryGreen,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ExamCard(
    examWithQuestions: ExamWithQuestions,
    onStart: () -> Unit,
    onDelete: () -> Unit
) {
    val exam = examWithQuestions.exam
    val formattedDate = remember(exam.createdAt) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(exam.createdAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LightGreen)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exam.title,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$formattedDate · ${examWithQuestions.questions.size} preguntas",
                        fontSize = 12.sp,
                        color = PrimaryGreen
                    )
                    exam.sourceFileName?.let { source ->
                        Text(
                            text = "Archivo: $source",
                            fontSize = 11.sp,
                            color = PrimaryGreen,
                            maxLines = 1
                        )
                    }
                }
                TextButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar examen",
                        tint = Color(0xFFC62828)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Guardado por: ${exam.ownerName.ifBlank { exam.userId }}",
                fontSize = 11.sp,
                color = PrimaryGreen
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Iniciar examen")
            }
        }
    }
}

@Composable
private fun QuestionItemCard(entity: com.example.preforge.data.local.QuestionEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LightGreen)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = entity.questionText,
                fontWeight = FontWeight.Bold,
                color = DarkGreen,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            entity.options.forEach { option ->
                val isCorrect = option == entity.correctAnswer
                Text(
                    text = option,
                    fontSize = 13.sp,
                    color = if (isCorrect) Color(0xFF2E7D32) else PrimaryGreen,
                    fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    userId: String,
    userName: String,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGreen)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = CircleShape,
            color = DarkGreen,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = userName,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen
        )
        Text(
            text = "Usuario: $userId",
            fontSize = 12.sp,
            color = PrimaryGreen,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, LightGreen)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Información de tu cuenta",
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                ProfileInfoRow("Almacenamiento", "Room SQLite")
                Spacer(modifier = Modifier.height(8.dp))
                ProfileInfoRow("Aislamiento", "Por usuario")
                Spacer(modifier = Modifier.height(8.dp))
                ProfileInfoRow("Estado", "Sesión local protegida")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            border = BorderStroke(1.5.dp, Color(0xFFC62828)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Cerrar Sesión",
                color = Color(0xFFC62828),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = PrimaryGreen)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    PreForgeTheme {
        MainScreen(
            userId = "preview-user",
            userName = "Estudiante"
        )
    }
}
