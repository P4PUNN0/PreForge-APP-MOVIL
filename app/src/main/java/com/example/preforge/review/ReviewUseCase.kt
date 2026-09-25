package com.example.preforge.review

import androidx.room.withTransaction
import com.example.preforge.data.local.AppDatabase
import com.example.preforge.data.local.ReviewAttemptDao
import com.example.preforge.data.local.ReviewAttemptEntity
import com.example.preforge.data.local.ReviewCardDao
import com.example.preforge.data.local.ReviewCardEntity
import kotlinx.coroutines.flow.Flow

/**
 * Orquesta el cálculo de SM-2 y la persistencia atómica de una respuesta.
 *
 * La tarjeta y el intento se guardan en la misma operación lógica. La base de
 * datos local queda como fuente de verdad; un sincronizador Firebase puede
 * replicar review_attempts y resolver conflictos por reviewedAt/updatedAt.
 */
class ReviewUseCase(
    private val database: AppDatabase,
    private val scheduler: SpacedRepetitionScheduler = SpacedRepetitionScheduler()
) {
    private val reviewCardDao: ReviewCardDao
        get() = database.reviewCardDao()

    private val reviewAttemptDao: ReviewAttemptDao
        get() = database.reviewAttemptDao()

    fun observeDueCards(
        userId: String,
        now: Long,
        limit: Int = ReviewCardDao.DEFAULT_DUE_CARD_LIMIT
    ): Flow<List<ReviewCardEntity>> = reviewCardDao.observeDueCards(
        userId = userId,
        now = now,
        limit = limit
    )

    suspend fun review(
        userId: String,
        questionId: Int,
        rating: Int,
        reviewedAt: Long = System.currentTimeMillis()
    ): ReviewCardEntity {
        require(userId.isNotBlank()) { "userId no puede estar vacío" }
        require(questionId > 0) { "questionId debe ser válido" }
        require(rating in ReviewSchedule.MIN_RATING..ReviewSchedule.MAX_RATING) {
            "La calificación debe estar entre ${ReviewSchedule.MIN_RATING} y ${ReviewSchedule.MAX_RATING}"
        }
        require(reviewedAt >= 0) { "reviewedAt no puede ser negativo" }

        return database.withTransaction {
            val storedCard = reviewCardDao.getByQuestion(userId, questionId)
            if (storedCard?.lastReviewedAt?.let { reviewedAt < it } == true) {
                return@withTransaction storedCard
            }
            val previousSchedule = storedCard?.toSchedule() ?: ReviewSchedule.dueAt(reviewedAt)
            val updatedSchedule = scheduler.calculate(
                previous = previousSchedule,
                rating = rating,
                reviewedAt = reviewedAt
            )
            val card = updatedSchedule.toEntity(
                questionId = questionId,
                userId = userId,
                id = storedCard?.id ?: 0,
                createdAt = storedCard?.createdAt ?: reviewedAt,
                updatedAt = reviewedAt
            )
            val cardId = reviewCardDao.insertOrUpdate(card)

            reviewAttemptDao.insert(
                ReviewAttemptEntity(
                    cardId = cardId,
                    userId = userId,
                    rating = rating,
                    reviewedAt = reviewedAt,
                    previousIntervalDays = previousSchedule.intervalDays,
                    newIntervalDays = updatedSchedule.intervalDays,
                    previousEaseFactor = previousSchedule.easeFactor,
                    newEaseFactor = updatedSchedule.easeFactor,
                    wasCorrect = rating >= ReviewSchedule.PASS_RATING
                )
            )

            card.copy(id = cardId)
        }
    }
}

private fun ReviewCardEntity.toSchedule(): ReviewSchedule = ReviewSchedule(
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    repetitions = repetitions,
    lapses = lapses,
    lastRating = lastRating,
    lastReviewedAt = lastReviewedAt,
    nextReviewAt = nextReviewAt
)

private fun ReviewSchedule.toEntity(
    questionId: Int,
    userId: String,
    id: Long,
    createdAt: Long,
    updatedAt: Long
): ReviewCardEntity = ReviewCardEntity(
    id = id,
    questionId = questionId,
    userId = userId,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    repetitions = repetitions,
    lapses = lapses,
    lastRating = lastRating,
    lastReviewedAt = lastReviewedAt,
    nextReviewAt = nextReviewAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)
