package com.example.preforge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReviewAttemptDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(attempt: ReviewAttemptEntity)

    @Query(
        """
        SELECT * FROM review_attempts
        WHERE card_id = :cardId
        ORDER BY reviewed_at DESC, id DESC
        """
    )
    suspend fun getForCard(cardId: Long): List<ReviewAttemptEntity>

    @Query(
        """
        SELECT * FROM review_attempts
        WHERE user_id = :userId
        ORDER BY reviewed_at DESC, id DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentForUser(
        userId: String,
        limit: Int = DEFAULT_ATTEMPT_LIMIT
    ): List<ReviewAttemptEntity>

    companion object {
        const val DEFAULT_ATTEMPT_LIMIT = 100
    }
}
