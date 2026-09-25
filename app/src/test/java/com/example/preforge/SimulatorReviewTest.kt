package com.example.preforge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulatorReviewTest {

    @Test
    fun `builds a complete review and marks unanswered questions as incorrect`() {
        val questions = listOf(
            Question(
                questionText = "¿Qué organelo produce ATP?",
                options = emptyList(),
                correctAnswer = "mitocondria",
                explanation = "La mitocondria produce ATP mediante fosforilación oxidativa."
            ),
            Question(
                questionText = "¿Cuál es la organela Digestiva?",
                options = emptyList(),
                correctAnswer = "retículo endoplásmico",
                explanation = "El retículo endoplásmico interviene en el procesamiento de proteínas y lípidos."
            )
        )
        val submittedAnswers = listOf(
            QuizAnswerAttempt(
                questionIndex = 0,
                question = questions.first(),
                selectedAnswer = "ribosoma",
                isCorrect = false
            )
        )

        val review = buildReviewAttempts(questions, submittedAnswers)

        assertEquals(2, review.size)
        assertEquals("ribosoma", review[0].selectedAnswer)
        assertFalse(review[0].isCorrect)
        assertNull(review[1].selectedAnswer)
        assertFalse(review[1].isCorrect)
    }

    @Test
    fun `keeps correct answers available before filtering them from the review`() {
        val question = Question(
            questionText = "¿La mitosis reparte el ADN?",
            options = listOf("Verdadero", "Falso"),
            correctAnswer = "Verdadero",
            questionType = QuestionType.TRUE_FALSE
        )
        val submittedAnswers = listOf(
            QuizAnswerAttempt(
                questionIndex = 0,
                question = question,
                selectedAnswer = "Verdadero",
                isCorrect = true
            )
        )

        val review = buildReviewAttempts(listOf(question), submittedAnswers)

        assertTrue(review.single().isCorrect)
    }
}
