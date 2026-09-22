package com.example.preforge

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AiEngine {
    //Aqui ponemos la API KEY de gemini
    private const val API_KEY = "YOUR_API_KEY_HERE"

    //Aqui ponemos el agente
    val generativeModel = GenerativeModel(
        modelName = "gemini-3.7-flash",
        apiKey = API_KEY
    )

    // Función multimodal que recibe la foto de la galería
    suspend fun generarCuestionarioDeImagen(imagen: Bitmap): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Eres un profesor experto. Lee los apuntes de esta imagen y crea un 
                    cuestionario de opción múltiple.
                    REGLA ESTRICTA: Tu respuesta debe ser ÚNICAMENTE un arreglo JSON válido, sin texto adicional ni saludos.
                    Usa exactamente esta estructura:
                    [
                      {
                        "topic": "Tema principal",
                        "text": "La pregunta aquí",
                        "optionA": "Respuesta A",
                        "optionB": "Respuesta B",
                        "optionC": "Respuesta C",
                        "optionD": "Respuesta D",
                        "correctAnswer": "A" 
                      }
                    ]
                """.trimIndent()

                // Así se le envía una imagen a Gemini
                val inputContent = content {
                    image(imagen)
                    text(prompt)
                }

                val response = generativeModel.generateContent(inputContent)
                response.text ?: "Error: Respuesta vacía."
            } catch (e: Exception) {
                "Error de conexión: ${e.message}"
            }
        }
    }
}