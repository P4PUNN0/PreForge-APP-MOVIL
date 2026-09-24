package com.example.preforge

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.RequestOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

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
    private const val NOMBRE_MODELO = "gemini-3.6-flash"
    private const val MAX_CARACTERES_APUNTES = 20_000
    private const val MAX_INTENTOS_GENERACION = 3
    private const val ESPERA_REINTENTO_MILLISEGUNDOS = 1_000L
    private const val TIEMPO_LIMITE_SOLICITUD_MILLISEGUNDOS = 120_000L
    private val claveApi = BuildConfig.GEMINI_API_KEY

    private val modeloGenerativo by lazy {
        GenerativeModel(
            modelName = NOMBRE_MODELO,
            apiKey = claveApi,
            generationConfig = GenerationConfig.builder().apply {
                candidateCount = 1
                temperature = 0.7f
                responseMimeType = "application/json"
            }.build(),
            requestOptions = RequestOptions(timeout = TIEMPO_LIMITE_SOLICITUD_MILLISEGUNDOS.seconds)
        )
    }

    suspend fun generateQuestions(
        contenidoArchivo: String,
        cantidadPreguntas: Int = 5
    ): List<Question> {
        if (claveApi.isBlank() || claveApi.contains("REPLACE")) {
            throw QuestionGenerationException(
                "La clave de Gemini no está configurada. Revisa local.properties"
            )
        }
        if (contenidoArchivo.isBlank()) {
            throw QuestionGenerationException("El archivo no contiene texto para generar preguntas")
        }

        val instruccion = construirInstruccionPregunta(contenidoArchivo, cantidadPreguntas)
        var ultimaExcepcion: Exception? = null

        repeat(MAX_INTENTOS_GENERACION) { intento ->
            try {
                Log.d(
                    "AiEngine",
                    "Solicitud $intento de $MAX_INTENTOS_GENERACION a Gemini ($NOMBRE_MODELO)"
                )
                val respuesta = modeloGenerativo.generateContent(instruccion)
                val textoJson = respuesta.text?.trim()
                if (textoJson.isNullOrBlank()) {
                    throw QuestionGenerationException("Gemini devolvió una respuesta vacía")
                }

                val preguntas = QuestionResponseParser.parse(
                    textoJson = textoJson,
                    cantidadEsperada = cantidadPreguntas
                )
                Log.d("AiEngine", "Se generaron ${preguntas.size} preguntas válidas")
                return preguntas
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (excepcion: Exception) {
                ultimaExcepcion = excepcion
                Log.w(
                    "AiEngine",
                    "La solicitud $intento falló: ${excepcion.message ?: excepcion::class.java.simpleName}"
                )
                if (intento < MAX_INTENTOS_GENERACION - 1) {
                    delay(ESPERA_REINTENTO_MILLISEGUNDOS * (intento + 1))
                }
            }
        }

        Log.e("AiEngine", "Gemini agotó los reintentos", ultimaExcepcion)
        throw QuestionGenerationException(
            "No se pudo generar un simulador válido con Gemini. Revisa tu conexión e inténtalo de nuevo",
            ultimaExcepcion
        )
    }

    private fun construirInstruccionPregunta(contenidoArchivo: String, cantidadPreguntas: Int): String = """
        Eres un experto creador de exámenes de estudio. Analiza únicamente el contenido de los apuntes proporcionados y genera exactamente $cantidadPreguntas preguntas de opción múltiple en español.

        REGLAS:
        1. Genera exactamente $cantidadPreguntas preguntas y cuatro opciones de respuesta por pregunta.
        2. Las cuatro opciones deben ser distintas, tener una longitud razonable y una sola debe ser correcta.
        3. Las preguntas deben ser claras y responder únicamente a información presente en los apuntes; no inventes datos.
        4. Evita repetir la misma idea o pregunta.
        5. No incluyas letras A), B), C) o D) dentro del campo "options"; la app añadirá las etiquetas.
        6. "correctAnswerIndex" es el índice de la opción correcta: 0 para la primera, 1 para la segunda, 2 para la tercera y 3 para la cuarta.
        7. Devuelve únicamente un arreglo JSON válido, sin bloques de código ni explicaciones.

        Formato exacto:
        [
          {
            "questionText": "¿Pregunta de ejemplo?",
            "options": ["Opción 1", "Opción 2", "Opción 3", "Opción 4"],
            "correctAnswerIndex": 0
          }
        ]

        Texto de apuntes:
        ${contenidoArchivo.take(MAX_CARACTERES_APUNTES)}
    """.trimIndent()
}
