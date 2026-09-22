package com.example.preforge.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions_table")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic: String,
    val text: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String
)