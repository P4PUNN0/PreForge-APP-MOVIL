package com.example.preforge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface QuestionDao {
    @Insert
    suspend fun insertQuestion(question: QuestionEntity)

    @Query("SELECT * FROM questions_table")
    suspend fun getAllQuestions(): List<QuestionEntity>
}