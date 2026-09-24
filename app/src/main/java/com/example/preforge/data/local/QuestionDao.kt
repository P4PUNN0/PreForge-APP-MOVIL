package com.example.preforge.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM preguntas WHERE exam_id = :examId ORDER BY id ASC")
    fun getQuestionsForExam(examId: Int): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM preguntas WHERE exam_id = :examId ORDER BY id ASC")
    suspend fun getQuestionsForExamList(examId: Int): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("DELETE FROM preguntas")
    suspend fun deleteAll()

    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)
}
