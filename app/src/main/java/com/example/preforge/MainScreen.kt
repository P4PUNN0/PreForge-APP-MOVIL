package com.example.preforge

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.preforge.data.local.QuestionEntity
import com.example.preforge.data.local.toQuestion
import com.example.preforge.ui.theme.*
import kotlinx.coroutines.launch

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun MainScreen(
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
                    userName = userName,
                    onNavigateToUpload = { currentTab = "upload" },
                    onNavigateToExams = { currentTab = "exams" }
                )
                "upload" -> DashboardScreen(
                    userName = userName,
                    onNavigateToSimulator = onNavigateToSimulator
                )
                "exams" -> ExamsScreen(
                    onNavigateToSimulator = onNavigateToSimulator
                )
                "profile" -> ProfileScreen(
                    userName = userName,
                    onLogout = onLogout
                )
            }
        }
    }
}

// --- PANTALLA: INICIO (HOME) ---
@Composable
fun HomeScreen(
    userName: String,
    onNavigateToUpload: () -> Unit,
    onNavigateToExams: () -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val savedQuestionsFlow = remember(isPreview) {
        if (!isPreview) {
            try {
                AppDatabase.getDatabase(context).questionDao().getAllQuestions()
            } catch (e: Exception) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        } else {
            kotlinx.coroutines.flow.flowOf(
                listOf(
                    QuestionEntity(id = 1, questionText = "Pregunta de muestra", options = listOf("A", "B"), correctAnswer = "A")
                )
            )
        }
    }
    val savedQuestions by savedQuestionsFlow.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGreen)
            .padding(24.dp)
    ) {
        // Banner de bienvenida
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
                    text = "Preparado para convertir tus apuntes en simuladores de examen de alta precisión.",
                    fontSize = 13.sp,
                    color = LightGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tarjeta de estadísticas rápidas Room
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
                    Text(text = "Estadísticas Local (Room)", fontSize = 12.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${savedQuestions.size} Preguntas en memoria",
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
            text = "Acciones Rápidas",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Opción 1: Generar Nuevo Examen
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
                    contentDescription = "Subir",
                    tint = DarkGreen,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Subir Archivo y Generar IA",
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Sube PDFs, Word o TXT para crear examen",
                        fontSize = 12.sp,
                        color = PrimaryGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Opción 2: Practicar con Guardados
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
                        text = "Ver Exámenes Guardados",
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Repasa las preguntas guardadas en tu telefono.",
                        fontSize = 12.sp,
                        color = PrimaryGreen
                    )
                }
            }
        }
    }
}

// --- PANTALLA: EXÁMENES (EXAMS) ---
@Composable
fun ExamsScreen(
    onNavigateToSimulator: (List<Question>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isPreview = LocalInspectionMode.current
    val savedQuestionsFlow = remember(isPreview) {
        if (!isPreview) {
            try {
                AppDatabase.getDatabase(context).questionDao().getAllQuestions()
            } catch (e: Exception) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        } else {
            kotlinx.coroutines.flow.flowOf(
                listOf(
                    QuestionEntity(
                        id = 1,
                        questionText = "¿Cuál es la capital de Francia?",
                        options = listOf("París", "Madrid", "Roma", "Berlín"),
                        correctAnswer = "París"
                    )
                )
            )
        }
    }
    val savedQuestionsEntities by savedQuestionsFlow.collectAsState(initial = emptyList())

    val questionsList = remember(savedQuestionsEntities) {
        savedQuestionsEntities.map { it.toQuestion() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGreen)
            .padding(24.dp)
    ) {
        Text(
            text = "Banco de Preguntas (Room)",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen
        )
        Text(
            text = "Preguntas almacenadas localmente en la base de datos de tu dispositivo.",
            fontSize = 13.sp,
            color = PrimaryGreen
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (questionsList.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { onNavigateToSimulator(questionsList) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Iniciar Examen (${questionsList.size})", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                AppDatabase.getDatabase(context).questionDao().deleteAll()
                                Toast.makeText(context, "Base de datos vaciada", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFC62828))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(savedQuestionsEntities) { entity ->
                    QuestionItemCard(entity = entity)
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aún no hay preguntas guardadas en Room.\n\nGenera un examen desde la sección 'Subir' para guardarlas aquí.",
                    textAlign = TextAlign.Center,
                    color = PrimaryGreen,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun QuestionItemCard(entity: QuestionEntity) {
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

// --- PANTALLA: PERFIL (PROFILE) ---
@Composable
fun ProfileScreen(
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
            text = "Estudiante PrepForge",
            fontSize = 14.sp,
            color = PrimaryGreen
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, LightGreen)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Información del Sistema", fontWeight = FontWeight.Bold, color = DarkGreen, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Almacenamiento:", fontSize = 14.sp, color = PrimaryGreen)
                    Text("Room SQLite", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Base de Datos:", fontSize = 14.sp, color = PrimaryGreen)
                    Text("preforge_database", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("IA Engine:", fontSize = 14.sp, color = PrimaryGreen)
                    Text("Gemini 3.6 Flash", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
                }
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
            Text("Cerrar Sesión", color = Color(0xFFC62828), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    PreForgeTheme {
        MainScreen(userName = "Estudiante")
    }
}
