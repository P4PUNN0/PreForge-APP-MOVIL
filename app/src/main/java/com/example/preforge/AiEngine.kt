package com.example.preforge

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.QuotaExceededException
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.ServerException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

internal object GeminiRequestPolicy {
    private val REGEX_ESPERA_REINTENTO =
        Regex("""retry\s+in\s+(\d+(?:\.\d+)?)\s*s""", RegexOption.IGNORE_CASE)
    private const val MAX_ESPERA_MILLISEGUNDOS = 60_000L
    private const val ESPERA_SATURACION_BASE_MILLISEGUNDOS = 5_000L

    fun retryDelayMillis(exception: Throwable, attemptIndex: Int): Long {
        val esperaIndicadaPorGemini = when (exception) {
            is QuotaExceededException -> exception.message
                ?.let(REGEX_ESPERA_REINTENTO::find)
                ?.groupValues
                ?.get(1)
                ?.toDoubleOrNull()
            else -> null
        }
        val esperaMilisegundos = when {
            esperaIndicadaPorGemini != null ->
                ceil(esperaIndicadaPorGemini).toLong() * 1_000L
            exception.isTemporaryHighDemand() ->
                ESPERA_SATURACION_BASE_MILLISEGUNDOS * (1L shl attemptIndex)
            else -> 1_000L * (attemptIndex + 1)
        }

        return esperaMilisegundos.coerceIn(1_000L, MAX_ESPERA_MILLISEGUNDOS)
    }

    private fun Throwable.isTemporaryHighDemand(): Boolean {
        if (this !is ServerException) return false
        val mensaje = message.orEmpty()
        return mensaje.contains("high demand", ignoreCase = true) ||
            mensaje.contains("temporarily unavailable", ignoreCase = true) ||
            mensaje.contains("try again later", ignoreCase = true)
    }

    fun userMessage(exception: Throwable?): String = when (exception) {
        is QuotaExceededException ->
            "Se alcanzó la cuota de uso de Gemini. Espera a que se libere o usa una clave de API con facturación habilitada."
        is ServerException -> if (exception.isTemporaryHighDemand()) {
            "Gemini está temporalmente saturado. Inténtalo de nuevo en unos minutos."
        } else {
            "No se pudo generar un simulador válido con Gemini. Revisa tu conexión e inténtalo de nuevo"
        }
        else ->
            "No se pudo generar un simulador válido con Gemini. Revisa tu conexión e inténtalo de nuevo"
    }
}

data class Question(
    val id: Int = 0,
    val questionType: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val questionText: String,
    val options: List<String>,
    val correctAnswer: String,
    val acceptedAnswers: List<String> = emptyList(),
    val explanation: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        questionText = parcel.readString() ?: "",
        options = parcel.createStringArrayList() ?: emptyList(),
        correctAnswer = parcel.readString() ?: "",
        questionType = if (parcel.dataAvail() > 0) {
            QuestionType.fromStorage(parcel.readString())
        } else {
            QuestionType.MULTIPLE_CHOICE
        },
        acceptedAnswers = if (parcel.dataAvail() > 0) {
            parcel.createStringArrayList() ?: emptyList()
        } else {
            emptyList()
        },
        explanation = if (parcel.dataAvail() > 0) {
            parcel.readString().orEmpty()
        } else {
            ""
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(questionText)
        parcel.writeStringList(options)
        parcel.writeString(correctAnswer)
        parcel.writeString(questionType.name)
        parcel.writeStringList(acceptedAnswers)
        parcel.writeString(explanation)
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
                    delay(GeminiRequestPolicy.retryDelayMillis(excepcion, intento))
                }
            }
        }

        Log.e("AiEngine", "Gemini agotó los reintentos", ultimaExcepcion)
        throw QuestionGenerationException(
            GeminiRequestPolicy.userMessage(ultimaExcepcion),
            ultimaExcepcion
        )
    }

    private fun construirInstruccionPregunta(contenidoArchivo: String, cantidadPreguntas: Int): String = """
        Eres un experto creador de exámenes de estudio. Analiza únicamente el contenido de los apuntes proporcionados y genera exactamente $cantidadPreguntas preguntas de opción múltiple en español.

        REGLAS:
        1. Genera exactamente $cantidadPreguntas preguntas y elige el formato más apropiado: MULTIPLE_CHOICE, TRUE_FALSE, OPEN o FILL_BLANKS.
        2. Para MULTIPLE_CHOICE, incluye exactamente cuatro opciones distintas y una sola correcta.
        3. Para TRUE_FALSE, usa las opciones "Verdadero" y "Falso" y devuelve la correcta.
        4. Para OPEN y FILL_BLANKS, devuelve "correctAnswer" y, si aplica, "acceptedAnswers" con variantes válidas.
        5. Incluye siempre "explanation" con dos o tres frases que expliquen por qué la respuesta correcta es correcta y qué concepto clave conviene reforzar. Basa la explicación únicamente en los apuntes y no menciones letras de opciones.
        6. Las preguntas deben ser claras y responder únicamente a información presente en los apuntes; no inventes datos.
        7. Evita repetir la misma idea o pregunta.
        8. No incluyas letras A), B), C) o D) dentro de "options"; la app las añadirá cuando el formato sea MULTIPLE_CHOICE.
        9. Devuelve únicamente un arreglo JSON válido, sin bloques de código ni explicaciones fuera del campo "explanation".

        Formatos:
        - MULTIPLE_CHOICE: questionType, questionText, options (4), correctAnswerIndex, explanation.
        - TRUE_FALSE: questionType, questionText, options ["Verdadero", "Falso"], correctAnswer, explanation.
        - OPEN/FILL_BLANKS: questionType, questionText, correctAnswer, acceptedAnswers opcional, explanation.

        Ejemplo:
        [
          {
            "questionType": "MULTIPLE_CHOICE",
            "questionText": "¿Pregunta de ejemplo?",
            "options": ["Opción 1", "Opción 2", "Opción 3", "Opción 4"],
            "correctAnswerIndex": 0,
            "explanation": "La primera opción es correcta porque recoge la relación principal entre el concepto y los apuntes. Conviene recordar esta relación para resolver preguntas similares."
          }
        ]

        Texto de apuntes:
        ${contenidoArchivo.take(MAX_CARACTERES_APUNTES)}
    """.trimIndent()
}
