package com.example.preforge.review

import androidx.room.Room
import com.example.preforge.data.local.AppDatabase
import com.example.preforge.data.local.ExamEntity
import com.example.preforge.data.local.QuestionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReviewUseCaseTest {

    @Test
    fun `persists card state and append-only attempt atomically`() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        try {
            val examId = database.examDao().insertExam(
                ExamEntity(
                    title = "Repaso",
                    userId = "user-1",
                    ownerName = "Estudiante",
                    questionCount = 1
                )
            ).toInt()
            val questionId = database.questionDao().insertQuestion(
                QuestionEntity(
                    questionText = "¿Qué es ATP?",
                    options = listOf("Energía", "ADN"),
                    correctAnswer = "Energía",
                    examId = examId
                )
            ).toInt()

            val useCase = ReviewUseCase(database)
            val card = useCase.review(
                userId = "user-1",
                questionId = questionId,
                rating = 4,
                reviewedAt = 1_000L
            )
            val storedCard = database.reviewCardDao()
                .getByQuestion("user-1", questionId)
            val attempts = database.reviewAttemptDao().getForCard(card.id)

            assertNotNull(storedCard)
            assertEquals(1, storedCard?.intervalDays)
            assertEquals(1, storedCard?.repetitions)
            assertEquals(1, attempts.size)
            assertEquals(4, attempts.single().rating)
            assertEquals(card.id, attempts.single().cardId)
        } finally {
            database.close()
        }
    }
}
