package com.example.preforge

/**
 * Formatos que puede renderizar el simulador.
 */
enum class QuestionType {
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    OPEN,
    FILL_BLANKS;

    companion object {
        fun fromStorage(value: String?): QuestionType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: MULTIPLE_CHOICE
    }
}
