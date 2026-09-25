package com.example.preforge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewCardDao {
    @Query(
        """
        SELECT * FROM review_cards
        WHERE user_id = :userId AND question_id = :questionId
        LIMIT 1
        """
    )
    suspend fun getByQuestion(userId: String, questionId: Int): ReviewCardEntity?

    @Query(
        """
        SELECT * FROM review_cards
        WHERE user_id = :userId AND next_review_at <= :now
        ORDER BY next_review_at ASC, id ASC
        LIMIT :limit
        """
    )
    fun observeDueCards(
        userId: String,
        now: Long,
        limit: Int = DEFAULT_DUE_CARD_LIMIT
    ): Flow<List<ReviewCardEntity>>

    @Query(
        """
        SELECT * FROM review_cards
        WHERE user_id = :userId AND next_review_at <= :now
        ORDER BY next_review_at ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getDueCards(
        userId: String,
        now: Long,
        limit: Int = DEFAULT_DUE_CARD_LIMIT
    ): List<ReviewCardEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(card: ReviewCardEntity): Long

    @Update
    suspend fun update(card: ReviewCardEntity)

    @Transaction
    suspend fun insertOrUpdate(card: ReviewCardEntity): Long {
        val insertedId = insertIfAbsent(card)
        if (insertedId != -1L) return insertedId

        val storedCard = getByQuestion(card.userId, card.questionId)
            ?: error("No se pudo guardar la tarjeta de repaso")
        update(card.copy(id = storedCard.id))
        return storedCard.id
    }

    companion object {
        const val DEFAULT_DUE_CARD_LIMIT = 50
    }
}
