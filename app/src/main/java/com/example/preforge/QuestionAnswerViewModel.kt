package com.example.preforge

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Estado de la respuesta de la pregunta actualmente visible. */
data class QuestionAnswerState(
    val answer: String? = null,
    val isSubmitted: Boolean = false,
    val isCorrect: Boolean? = null,
    val validationMessage: String? = null
)

/**
 * ViewModel pequeno para no mezclar la validacion de una pregunta con el
 * cronometro y la navegacion del simulador.
 */
class QuestionAnswerViewModel : ViewModel() {
    private val _answerState = MutableStateFlow(QuestionAnswerState())
    val answerState: StateFlow<QuestionAnswerState> = _answerState.asStateFlow()

    fun updateDraft(answer: String) {
        if (!_answerState.value.isSubmitted) {
            _answerState.value = _answerState.value.copy(
                answer = answer,
                validationMessage = null
            )
        }
    }

    fun submit(question: Question, answer: String): Boolean {
        if (answer.isBlank()) {
            _answerState.value = QuestionAnswerState(
                answer = answer,
                validationMessage = "Escribe una respuesta antes de comprobar"
            )
            return false
        }

        val correct = isAnswerCorrect(question, answer)
        _answerState.value = QuestionAnswerState(
            answer = answer,
            isSubmitted = true,
            isCorrect = correct,
            validationMessage = null
        )
        return correct
    }

    fun reset() {
        _answerState.value = QuestionAnswerState()
    }

    private fun isAnswerCorrect(question: Question, answer: String): Boolean =
        when (question.questionType) {
            QuestionType.MULTIPLE_CHOICE,
            QuestionType.TRUE_FALSE -> normalize(answer) == normalize(question.correctAnswer)

            QuestionType.OPEN,
            QuestionType.FILL_BLANKS -> {
                val validAnswers = question.acceptedAnswers + question.correctAnswer
                validAnswers.any { normalize(it) == normalize(answer) }
            }
        }

    private fun normalize(value: String): String = value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
}
