package com.example.preforge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Transaction
    @Query("SELECT * FROM exams WHERE user_id = :userId ORDER BY created_at DESC, id DESC")
    fun observeExamsForUser(userId: String): Flow<List<ExamWithQuestions>>

    @Transaction
    @Query("SELECT * FROM exams WHERE user_id = :userId ORDER BY created_at DESC, id DESC LIMIT 1")
    suspend fun getLatestExamForUser(userId: String): ExamWithQuestions?

    @Transaction
    @Query("SELECT * FROM exams WHERE id = :examId LIMIT 1")
    suspend fun getExam(examId: Int): ExamWithQuestions?

    @Query("SELECT COUNT(*) FROM exams WHERE user_id = :userId")
    fun observeExamCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExam(exam: ExamEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("DELETE FROM exams WHERE id = :examId AND user_id = :userId")
    suspend fun deleteExam(examId: Int, userId: String)

    @Query("DELETE FROM exams WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Transaction
    suspend fun insertExamWithQuestions(
        exam: ExamEntity,
        questions: List<QuestionEntity>
    ): Long {
        val examId = insertExam(exam)
        insertQuestions(questions.map { question ->
            question.copy(examId = examId.toInt())
        })
        return examId
    }
}
