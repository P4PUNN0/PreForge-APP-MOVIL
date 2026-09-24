package com.example.preforge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuestionResponseParserTest {

    @Test
    fun `parses valid questions and assigns ordered labels after shuffling`() {
        val respuesta = """
            [
              {
                "questionText": "What organ produces ATP?",
                "options": ["La mitocondria", "El núcleo", "El ribosoma", "El retículo endoplásmico"],
                "correctAnswerIndex": 0
              }
            ]
        """.trimIndent()

        val pregunta = QuestionResponseParser.parse(
            textoJson = respuesta,
            cantidadEsperada = 1,
            generadorAleatorio = Random(7)
        ).single()

        assertEquals("What organ produces ATP?", pregunta.questionText)
        assertEquals(4, pregunta.options.size)
        assertEquals(
            listOf("A", "B", "C", "D"),
            pregunta.options.map { it.substringBefore(") ") }
        )
        assertTrue(pregunta.correctAnswer in pregunta.options)
    }

    @Test
    fun `rejects a response with the wrong number of options`() {
        val respuesta = """
            [
              {
                "questionText": "Pregunta inválida",
                "options": ["Uno", "Dos", "Tres"],
                "correctAnswerIndex": 0
              }
            ]
        """.trimIndent()

        assertThrows(QuestionGenerationException::class.java) {
            QuestionResponseParser.parse(respuesta, cantidadEsperada = 1)
        }
    }
}
