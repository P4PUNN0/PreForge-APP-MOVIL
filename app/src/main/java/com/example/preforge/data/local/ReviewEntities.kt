package com.example.preforge.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Estado de repetición de una pregunta para un usuario.
 *
 * Se separa de QuestionEntity porque el mismo contenido puede aparecer en
 * varios exámenes y cada estudiante tiene un calendario diferente.
 */
@Entity(
    tableName = "review_cards",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id", "question_id"], unique = true),
        Index(value = ["user_id", "next_review_at"]),
        Index(value = ["question_id"])
    ]
)
data class ReviewCardEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "question_id")
    val questionId: Int,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "ease_factor")
    val easeFactor: Double = 2.5,

    @ColumnInfo(name = "interval_days")
    val intervalDays: Int = 0,

    @ColumnInfo(name = "repetitions")
    val repetitions: Int = 0,

    @ColumnInfo(name = "lapses")
    val lapses: Int = 0,

    @ColumnInfo(name = "last_rating")
    val lastRating: Int? = null,

    @ColumnInfo(name = "last_reviewed_at")
    val lastReviewedAt: Long? = null,

    @ColumnInfo(name = "next_review_at")
    val nextReviewAt: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Evento append-only de cada respuesta. Permite auditar el cambio y facilita
 * una futura sincronización con Firebase sin perder historial.
 */
@Entity(
    tableName = "review_attempts",
    foreignKeys = [
        ForeignKey(
            entity = ReviewCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["card_id"]),
        Index(value = ["user_id", "reviewed_at"])
    ]
)
data class ReviewAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "card_id")
    val cardId: Long,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "rating")
    val rating: Int,

    @ColumnInfo(name = "reviewed_at")
    val reviewedAt: Long,

    @ColumnInfo(name = "previous_interval_days")
    val previousIntervalDays: Int,

    @ColumnInfo(name = "new_interval_days")
    val newIntervalDays: Int,

    @ColumnInfo(name = "previous_ease_factor")
    val previousEaseFactor: Double,

    @ColumnInfo(name = "new_ease_factor")
    val newEaseFactor: Double,

    @ColumnInfo(name = "was_correct")
    val wasCorrect: Boolean
)
