package com.example.preforge

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.random.Random

internal class QuestionGenerationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

internal object QuestionResponseParser {
    private const val CANTIDAD_OPCIONES = 4
    private const val MENSAJE_RESPUESTA_INVALIDA =
        "La IA no devolvió una respuesta válida para una de las preguntas"

    fun parse(
        textoJson: String,
        cantidadEsperada: Int,
        generadorAleatorio: Random = Random.Default
    ): List<Question> {
        if (cantidadEsperada <= 0) {
            throw QuestionGenerationException("La cantidad de preguntas debe ser mayor que cero")
        }

        val arregloJson = analizarArregloJson(textoJson)
        if (arregloJson.length() != cantidadEsperada) {
            throw QuestionGenerationException(
                "La IA devolvió ${arregloJson.length()} preguntas y se solicitaron $cantidadEsperada"
            )
        }

        return buildList(cantidadEsperada) {
            for (indice in 0 until arregloJson.length()) {
                add(analizarPregunta(arregloJson.optJSONObject(indice), generadorAleatorio))
            }
        }
    }

    private fun analizarArregloJson(textoJson: String): JSONArray {
        val textoRecortado = textoJson.trim()
        val indiceInicio = textoRecortado.indexOf('[')
        val indiceFin = textoRecortado.lastIndexOf(']')
        if (indiceInicio == -1 || indiceFin < indiceInicio) {
            throw QuestionGenerationException(MENSAJE_RESPUESTA_INVALIDA)
        }

        return try {
            JSONArray(textoRecortado.substring(indiceInicio, indiceFin + 1))
        } catch (excepcion: Exception) {
            throw QuestionGenerationException(MENSAJE_RESPUESTA_INVALIDA, excepcion)
        }
    }

    private fun analizarPregunta(
        objetoPregunta: JSONObject?,
        generadorAleatorio: Random
    ): Question {
        if (objetoPregunta == null) {
            throw QuestionGenerationException("La IA devolvió un elemento de pregunta inválido")
        }
        val textoPregunta = objetoPregunta.optString("questionText").trim()
        if (textoPregunta.isBlank()) {
            throw QuestionGenerationException("La IA devolvió una pregunta vacía")
        }

        val questionType = analizarTipoPregunta(objetoPregunta)
        val pregunta = when (questionType) {
            QuestionType.MULTIPLE_CHOICE -> analizarOpcionMultiple(
                objetoPregunta = objetoPregunta,
                textoPregunta = textoPregunta,
                generadorAleatorio = generadorAleatorio
            )

            QuestionType.TRUE_FALSE -> analizarVerdaderoFalso(
                objetoPregunta = objetoPregunta,
                textoPregunta = textoPregunta
            )

            QuestionType.OPEN,
            QuestionType.FILL_BLANKS -> analizarRespuestaAbierta(
                objetoPregunta = objetoPregunta,
                textoPregunta = textoPregunta,
                questionType = questionType
            )
        }

        val explicacion = objetoPregunta.optString("explanation").trim()
        if (explicacion.isBlank()) {
            throw QuestionGenerationException("Falta la explicación de una pregunta")
        }
        return pregunta.copy(explanation = explicacion)
    }

    private fun analizarTipoPregunta(objetoPregunta: JSONObject): QuestionType {
        val valor = objetoPregunta.optString("questionType").trim()
        if (valor.isBlank()) return QuestionType.MULTIPLE_CHOICE
        return QuestionType.entries.firstOrNull { it.name.equals(valor, ignoreCase = true) }
            ?: throw QuestionGenerationException("La IA devolvió un tipo de pregunta desconocido")
    }

    private fun analizarOpcionMultiple(
        objetoPregunta: JSONObject,
        textoPregunta: String,
        generadorAleatorio: Random
    ): Question {
        val arregloOpciones = objetoPregunta.optJSONArray("options")
            ?: throw QuestionGenerationException("Falta la lista de opciones de una pregunta")
        val indiceRespuestaCorrecta = objetoPregunta.optInt("correctAnswerIndex", -1)
        if (arregloOpciones.length() != CANTIDAD_OPCIONES) {
            throw QuestionGenerationException("Una pregunta no tiene exactamente cuatro opciones")
        }
        if (indiceRespuestaCorrecta !in 0 until arregloOpciones.length()) {
            throw QuestionGenerationException("Una pregunta tiene un índice de respuesta inválido")
        }

        val opciones = arregloOpciones.toStringList()
        if (opciones.any(String::isBlank)) {
            throw QuestionGenerationException("La IA devolvió una opción vacía")
        }
        if (opciones.map { it.lowercase(Locale.ROOT) }.distinct().size != opciones.size) {
            throw QuestionGenerationException("La IA devolvió opciones repetidas")
        }

        val respuestaCorrecta = opciones[indiceRespuestaCorrecta]
        val opcionesBarajadas = opciones.shuffled(generadorAleatorio)
        val opcionesEtiquetadas = opcionesBarajadas.mapIndexed { indice, opcion ->
            "${('A'.code + indice).toChar()}) $opcion"
        }
        val indiceCorrectaBarajada = opcionesBarajadas.indexOf(respuestaCorrecta)
        return Question(
            questionType = QuestionType.MULTIPLE_CHOICE,
            questionText = textoPregunta,
            options = opcionesEtiquetadas,
            correctAnswer = opcionesEtiquetadas[indiceCorrectaBarajada]
        )
    }

    private fun analizarVerdaderoFalso(
        objetoPregunta: JSONObject,
        textoPregunta: String
    ): Question {
        val opciones = objetoPregunta.optJSONArray("options")?.toStringList()
            ?.takeIf { it.size == 2 }
            ?: listOf("Verdadero", "Falso")
        val indiceRespuestaCorrecta = objetoPregunta.optInt("correctAnswerIndex", -1)
        val respuestaCorrecta = opciones.getOrNull(indiceRespuestaCorrecta)
            ?: objetoPregunta.optString("correctAnswer").trim()
        if (respuestaCorrecta !in opciones) {
            throw QuestionGenerationException(
                "La respuesta verdadero/falso no coincide con las opciones"
            )
        }
        return Question(
            questionType = QuestionType.TRUE_FALSE,
            questionText = textoPregunta,
            options = opciones,
            correctAnswer = respuestaCorrecta
        )
    }

    private fun analizarRespuestaAbierta(
        objetoPregunta: JSONObject,
        textoPregunta: String,
        questionType: QuestionType
    ): Question {
        val respuestaCorrecta = objetoPregunta.optString("correctAnswer").trim()
        if (respuestaCorrecta.isBlank()) {
            throw QuestionGenerationException("Falta la respuesta de una pregunta abierta")
        }
        val respuestasAceptadas = objetoPregunta.optJSONArray("acceptedAnswers")
            ?.toStringList()
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            .orEmpty()
        return Question(
            questionType = questionType,
            questionText = textoPregunta,
            options = emptyList(),
            correctAnswer = respuestaCorrecta,
            acceptedAnswers = respuestasAceptadas
        )
    }

    private fun JSONArray.toStringList(): List<String> = buildList(length()) {
        for (indice in 0 until length()) {
            add(optString(indice).trim())
        }
    }
}
