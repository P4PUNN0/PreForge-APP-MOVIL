package com.example.preforge

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import com.example.preforge.data.local.AppDatabase
import com.example.preforge.data.local.QuestionEntity
import com.example.preforge.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray // Importante para leer el JSON

@Composable
fun DashboardScreen(onNavigateToSimulator: () -> Unit = {}) {
    // Memoria de la pantalla
    var respuestaIA by remember { mutableStateOf("") }
    var estaCargando by remember { mutableStateOf(false) }
    var totalPreguntasGuardadas by remember { mutableStateOf(0) }

    // Herramientas para corrutinas y contexto
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var bitmapSeleccionado by remember { mutableStateOf<Bitmap?>(null) }

    // Inicializar la base de datos Room
    val db = remember {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "preforge_database"
        ).build()
    }
    val questionDao = db.questionDao()

    // Lanzador para la galería de fotos
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            bitmapSeleccionado = if (Build.VERSION.SDK_INT >= 28) {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }

            // Mandamos la foto a Gemini y procesamos la respuesta de forma segura
            coroutineScope.launch {
                estaCargando = true
                try {
                    val respuestaCruda = AiEngine.generarCuestionarioDeImagen(bitmapSeleccionado!!)

                    // Buscamos dónde empieza el arreglo '[' y dónde termina ']'
                    val inicio = respuestaCruda.indexOf('[')
                    val fin = respuestaCruda.lastIndexOf(']')

                    if (inicio != -1 && fin != -1 && inicio < fin) {
                        // Extraemos SOLO la parte que es JSON puro
                        val jsonPuro = respuestaCruda.substring(inicio, fin + 1)
                        val jsonArray = JSONArray(jsonPuro)

                        // Guardamos en Room
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val nuevaPregunta = QuestionEntity(
                                topic = obj.getString("topic"),
                                text = obj.getString("text"),
                                optionA = obj.getString("optionA"),
                                optionB = obj.getString("optionB"),
                                optionC = obj.getString("optionC"),
                                optionD = obj.getString("optionD"),
                                correctAnswer = obj.getString("correctAnswer")
                            )
                            questionDao.insertQuestion(nuevaPregunta)
                        }

                        val listaDePreguntas = questionDao.getAllQuestions()
                        totalPreguntasGuardadas = listaDePreguntas.size
                        respuestaIA = "¡Éxito! Se generaron y guardaron ${jsonArray.length()} preguntas nuevas en tu base de datos local."
                    } else {
                        // Si no hay JSON, mostramos lo que contestó el servidor para diagnosticar el problema
                        respuestaIA = "Respuesta del servidor:\n$respuestaCruda"
                    }

                } catch (e: Exception) {
                    respuestaIA = "Error al procesar el texto: ${e.message}"
                }
                estaCargando = false
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGreen)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Encabezado del usuario
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Person, contentDescription = "Perfil", tint = DarkGreen, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "¡Hola, Estudiante!", fontSize = 12.sp, color = PrimaryGreen)
                Text(text = "María García", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Sube tus apuntes", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
        Text(text = "Sube la foto de tu libreta para que la IA genere el simulador.", fontSize = 14.sp, color = PrimaryGreen)

        Spacer(modifier = Modifier.height(24.dp))

        // Botón para subir imagen desde Galería
        Button(
            onClick = {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Subir Apunte (Galería)", color = Color.White, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mostrar la ruedita de carga o la tarjeta con la respuesta
        if (estaCargando) {
            CircularProgressIndicator(
                color = DarkGreen,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else if (respuestaIA.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = respuestaIA,
                    modifier = Modifier.padding(16.dp),
                    color = DarkGreen,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para probar Room Localmente
        Button(
            onClick = {
                coroutineScope.launch {
                    val preguntaSimulada = QuestionEntity(
                        topic = "Biología",
                        text = "¿Qué es la fotosíntesis?",
                        optionA = "Un proceso metabólico",
                        optionB = "Un animal",
                        optionC = "Un mineral",
                        optionD = "Un planeta",
                        correctAnswer = "A"
                    )

                    questionDao.insertQuestion(preguntaSimulada)
                    val listaDePreguntas = questionDao.getAllQuestions()
                    totalPreguntasGuardadas = listaDePreguntas.size
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Probar Guardado Local (Room)", color = Color.White, fontSize = 16.sp)
        }

        if (totalPreguntasGuardadas > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "¡Éxito! Preguntas guardadas en SQLite: $totalPreguntasGuardadas",
                color = DarkGreen,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón original para ir al simulador de forma manual
        OutlinedButton(
            onClick = onNavigateToSimulator,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Ir al Simulador Manual", color = DarkGreen, fontSize = 16.sp)
        }
    }
}