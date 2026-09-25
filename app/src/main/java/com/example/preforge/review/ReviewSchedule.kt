package com.example.preforge.review

/**
 * Estado de scheduling de una tarjeta para un estudiante concreto.
 *
 * Las fechas se guardan como epoch milliseconds para no depender de java.time
 * en dispositivos Android 7.
 */
data class ReviewSchedule(
    val easeFactor: Double = INITIAL_EASE_FACTOR,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val lastRating: Int? = null,
    val lastReviewedAt: Long? = null,
    val nextReviewAt: Long
) {
    init {
        require(easeFactor in MIN_EASE_FACTOR..MAX_EASE_FACTOR) {
            "easeFactor debe estar entre $MIN_EASE_FACTOR y $MAX_EASE_FACTOR"
        }
        require(intervalDays >= 0) { "intervalDays no puede ser negativo" }
        require(repetitions >= 0) { "repetitions no puede ser negativo" }
        require(lapses >= 0) { "lapses no puede ser negativo" }
        require(lastRating == null || lastRating in MIN_RATING..MAX_RATING) {
            "lastRating debe estar entre $MIN_RATING y $MAX_RATING"
        }
    }

    companion object {
        const val MIN_RATING = 0
        const val MAX_RATING = 5
        const val PASS_RATING = 3
        const val MIN_EASE_FACTOR = 1.3
        const val INITIAL_EASE_FACTOR = 2.5
        const val MAX_EASE_FACTOR = 3.0
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

        fun dueAt(now: Long): ReviewSchedule = ReviewSchedule(nextReviewAt = now)
    }
}
