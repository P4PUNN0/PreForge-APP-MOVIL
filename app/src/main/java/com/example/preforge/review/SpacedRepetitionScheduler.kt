package com.example.preforge.review

import kotlin.math.roundToInt

/**
 * Variante de SM-2 para autoevaluaciones de 0 a 5.
 *
 * La fórmula de facilidad es:
 * EF' = EF + 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)
 *
 * Una calificación menor a 3 reinicia el intervalo y las repeticiones, pero
 * conserva la facilidad, personalizándola para la recuperación. Las calificaciones
 * aprobadas usan 1 dia, 6 dias y luego el intervalo anterior multiplicado por
 * la nueva facilidad.
 */
class SpacedRepetitionScheduler {

    fun calculate(
        previous: ReviewSchedule,
        rating: Int,
        reviewedAt: Long
    ): ReviewSchedule {
        require(rating in ReviewSchedule.MIN_RATING..ReviewSchedule.MAX_RATING) {
            "La calificación debe estar entre ${ReviewSchedule.MIN_RATING} y ${ReviewSchedule.MAX_RATING}"
        }
        require(reviewedAt >= 0) { "reviewedAt no puede ser negativo" }

        val easeFactor = calculateEaseFactor(previous.easeFactor, rating)
        if (rating < ReviewSchedule.PASS_RATING) {
            return previous.copy(
                easeFactor = easeFactor,
                intervalDays = FAILURE_INTERVAL_DAYS,
                repetitions = 0,
                lapses = previous.lapses + 1,
                lastRating = rating,
                lastReviewedAt = reviewedAt,
                nextReviewAt = addDays(reviewedAt, FAILURE_INTERVAL_DAYS)
            )
        }

        val intervalDays = calculateSuccessfulInterval(
            previousRepetitions = previous.repetitions,
            previousIntervalDays = previous.intervalDays,
            easeFactor = easeFactor
        )
        return previous.copy(
            easeFactor = easeFactor,
            intervalDays = intervalDays,
            repetitions = previous.repetitions + 1,
            lastRating = rating,
            lastReviewedAt = reviewedAt,
            nextReviewAt = addDays(reviewedAt, intervalDays)
        )
    }

    private fun calculateEaseFactor(previousEaseFactor: Double, rating: Int): Double {
        val difficulty = 5 - rating
        val delta = 0.1 - difficulty * (0.08 + difficulty * 0.02)
        return (previousEaseFactor + delta).coerceIn(
            ReviewSchedule.MIN_EASE_FACTOR,
            ReviewSchedule.MAX_EASE_FACTOR
        )
    }

    private fun calculateSuccessfulInterval(
        previousRepetitions: Int,
        previousIntervalDays: Int,
        easeFactor: Double
    ): Int = when (previousRepetitions) {
        0 -> FIRST_INTERVAL_DAYS
        1 -> SECOND_INTERVAL_DAYS
        else -> (previousIntervalDays * easeFactor).roundToInt()
    }.coerceIn(MINIMUM_INTERVAL_DAYS, MAXIMUM_INTERVAL_DAYS)

    private fun addDays(timestamp: Long, days: Int): Long {
        val duration = days.toLong() * ReviewSchedule.MILLIS_PER_DAY
        return if (timestamp > Long.MAX_VALUE - duration) {
            Long.MAX_VALUE
        } else {
            timestamp + duration
        }
    }

    private companion object {
        const val FAILURE_INTERVAL_DAYS = 1
        const val FIRST_INTERVAL_DAYS = 1
        const val SECOND_INTERVAL_DAYS = 6
        const val MINIMUM_INTERVAL_DAYS = 1
        const val MAXIMUM_INTERVAL_DAYS = 3650
    }
}
