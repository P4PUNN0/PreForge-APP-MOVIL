package com.example.preforge

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import org.json.JSONArray

data class Question(
    val questionText: String,
    val options: List<String>,
    val correctAnswer: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.createStringArrayList() ?: emptyList(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(questionText)
        parcel.writeStringList(options)
        parcel.writeString(correctAnswer)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Question> {
        override fun createFromParcel(parcel: Parcel): Question = Question(parcel)
        override fun newArray(size: Int): Array<Question?> = arrayOfNulls(size)
    }
}

object AiEngine {
    // API Key de Gemini (Reemplaza con tu clave API)
    private const val API_KEY = "YOUR_GEMINI_API_KEY"

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-3.6-flash",
            apiKey = API_KEY
        )
    }

    suspend fun generateQuestions(fileContent: String): List<Question> {
        if (API_KEY.isBlank() || API_KEY.contains("REPLACE")) {
            return getFallbackQuestions("La API Key no está configurada correctamente.")
        }

        val prompt = """
            Eres un experto creador de exámenes de estudio. Analiza el siguiente texto de apuntes del estudiante y genera exactamente 5 preguntas de opción múltiple con 4 opciones de respuesta cada una. Una de ellas debe ser la correcta.
            Devuelve ÚNICAMENTE un arreglo JSON válido, sin bloques de código con ```json ni texto adicional. El formato del JSON debe ser exactamente el siguiente:
            [
              {
                "questionText": "¿Pregunta de ejemplo?",
                "options": ["A) Opción 1", "B) Opción 2", "C) Opción 3", "D) Opción 4"],
                "correctAnswer": "A) Opción 1"
              }
            ]
            
            Texto de apuntes:
            ${fileContent.take(5000)}
        """.trimIndent()

        return try {
            Log.d("AiEngine", "Enviando solicitud a Gemini...")
            val response = generativeModel.generateContent(prompt)
            val jsonText = response.text?.trim()

            Log.d("AiEngine", "Respuesta recibida de Gemini: $jsonText")

            if (jsonText.isNullOrBlank()) {
                Log.e("AiEngine", "La respuesta de Gemini fue nula o vacía.")
                return getFallbackQuestions("La respuesta de Gemini fue nula o vacía.")
            }

            // Búsqueda inteligente de corchetes para extraer el JSON
            val jsonStartIndex = jsonText.indexOf("[")
            val jsonEndIndex = jsonText.lastIndexOf("]")

            if (jsonStartIndex == -1 || jsonEndIndex == -1 || jsonEndIndex < jsonStartIndex) {
                Log.e("AiEngine", "No se encontró un formato JSON de arreglo válido.")
                return getFallbackQuestions("No se pudo estructurar el JSON con la respuesta recibida.")
            }

            val cleanJson = jsonText.substring(jsonStartIndex, jsonEndIndex + 1)
            val jsonArray = JSONArray(cleanJson)
            val questions = mutableListOf<Question>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val questionText = obj.getString("questionText")
                val optionsArray = obj.getJSONArray("options")
                val options = mutableListOf<String>()
                for (j in 0 until optionsArray.length()) {
                    options.add(optionsArray.getString(j))
                }
                val correctAnswer = obj.getString("correctAnswer")
                questions.add(Question(questionText, options, correctAnswer))
            }

            if (questions.isEmpty()) {
                Log.w("AiEngine", "El arreglo de preguntas parseado quedó vacío.")
                getFallbackQuestions("No se encontraron preguntas en el archivo.")
            } else {
                Log.d("AiEngine", "¡Éxito! Se generaron ${questions.size} preguntas correctamente.")
                questions
            }
        } catch (e: Exception) {
            Log.e("AiEngine", "Error de conexión o procesamiento en Gemini: ${e.message}", e)
            getFallbackQuestions("Error de API: ${e.localizedMessage ?: "Fallo al conectar con Gemini"}")
        }
    }

    private fun getFallbackQuestions(errorMessage: String): List<Question> {
        return listOf(
            Question(
                questionText = errorMessage,
                options = listOf("Opción A", "Opción B", "Opción C", "Opción D"),
                correctAnswer = "Opción A"
            )
        )
    }
}