package com.example.preforge

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionAnswerViewModelTest {

    @Test
    fun `evaluates true false answers`() {
        val viewModel = QuestionAnswerViewModel()
        val question = Question(
            questionType = QuestionType.TRUE_FALSE,
            questionText = "La mitosis produce dos células",
            options = listOf("Verdadero", "Falso"),
            correctAnswer = "Verdadero"
        )

        assertTrue(viewModel.submit(question, "verdadero"))
        assertTrue(viewModel.answerState.value.isSubmitted)
        assertTrue(viewModel.answerState.value.isCorrect == true)
    }

    @Test
    fun `accepts an alternate answer for an open question`() {
        val viewModel = QuestionAnswerViewModel()
        val question = Question(
            questionType = QuestionType.OPEN,
            questionText = "¿Qué organelo produce ATP?",
            options = emptyList(),
            correctAnswer = "mitocondria",
            acceptedAnswers = listOf("la mitocondria")
        )

        assertTrue(viewModel.submit(question, "  La   mitocondria "))
        assertFalse(viewModel.submit(question, "ribosoma"))
    }
}
