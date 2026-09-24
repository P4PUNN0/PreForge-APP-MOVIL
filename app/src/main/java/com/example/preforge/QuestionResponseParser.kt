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
                add(analizarPregunta(arregloJson.getJSONObject(indice), generadorAleatorio))
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
        objetoPregunta: JSONObject,
        generadorAleatorio: Random
    ): Question {
        val textoPregunta = objetoPregunta.optString("questionText").trim()
        val arregloOpciones = objetoPregunta.optJSONArray("options")
            ?: throw QuestionGenerationException("Falta la lista de opciones de una pregunta")
        val indiceRespuestaCorrecta = objetoPregunta.optInt("correctAnswerIndex", -1)

        if (textoPregunta.isBlank()) {
            throw QuestionGenerationException("La IA devolvió una pregunta vacía")
        }
        if (arregloOpciones.length() != CANTIDAD_OPCIONES) {
            throw QuestionGenerationException("Una pregunta no tiene exactamente cuatro opciones")
        }
        if (indiceRespuestaCorrecta !in 0 until arregloOpciones.length()) {
            throw QuestionGenerationException("Una pregunta tiene un índice de respuesta inválido")
        }

        val opciones = buildList {
            for (indiceOpcion in 0 until arregloOpciones.length()) {
                add(arregloOpciones.optString(indiceOpcion).trim())
            }
        }

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
        val respuestaCorrectaEtiquetada = opcionesEtiquetadas[indiceCorrectaBarajada]

        return Question(
            questionText = textoPregunta,
            options = opcionesEtiquetadas,
            correctAnswer = respuestaCorrectaEtiquetada
        )
    }
}
