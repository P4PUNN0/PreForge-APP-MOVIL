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
                "correctAnswerIndex": 0,
                "explanation": "La mitocondria transforma la energía de los nutrientes en ATP, que la célula utiliza como fuente inmediata de energía."
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
        assertEquals(
            "La mitocondria transforma la energía de los nutrientes en ATP, que la célula utiliza como fuente inmediata de energía.",
            pregunta.explanation
        )
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

    @Test
    fun `parses true false questions`() {
        val respuesta = """
            [
              {
                "questionType": "TRUE_FALSE",
                "questionText": "La mitocondria produce ATP",
                "options": ["Verdadero", "Falso"],
                "correctAnswer": "Verdadero",
                "explanation": "La mitocondria participa en la fosforilación oxidativa, el proceso que produce gran parte del ATP celular."
              }
            ]
        """.trimIndent()

        val pregunta = QuestionResponseParser.parse(respuesta, 1).single()

        assertEquals(QuestionType.TRUE_FALSE, pregunta.questionType)
        assertEquals(listOf("Verdadero", "Falso"), pregunta.options)
        assertEquals("Verdadero", pregunta.correctAnswer)
    }

    @Test
    fun `parses open questions with accepted answers`() {
        val respuesta = """
            [
              {
                "questionType": "OPEN",
                "questionText": "¿Cómo se llama el proceso?",
                "correctAnswer": "respiración celular",
                "acceptedAnswers": ["Respiracion celular"],
                "explanation": "La respiración celular obtiene energía a partir de los nutrientes y produce ATP para las funciones de la célula."
              }
            ]
        """.trimIndent()

        val pregunta = QuestionResponseParser.parse(respuesta, 1).single()

        assertEquals(QuestionType.OPEN, pregunta.questionType)
        assertEquals("respiración celular", pregunta.correctAnswer)
        assertEquals(listOf("Respiracion celular"), pregunta.acceptedAnswers)
    }

    @Test
    fun `rejects a question without intelligent feedback`() {
        val respuesta = """
            [
              {
                "questionType": "TRUE_FALSE",
                "questionText": "La mitocondria produce ATP",
                "options": ["Verdadero", "Falso"],
                "correctAnswer": "Verdadero"
              }
            ]
        """.trimIndent()

        assertThrows(QuestionGenerationException::class.java) {
            QuestionResponseParser.parse(respuesta, cantidadEsperada = 1)
        }
    }
}
