package com.example.preforge.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.preforge.Question
import org.json.JSONArray

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val questionText: String,
    val options: List<String>,
    val correctAnswer: String,
    val createdAt: Long = System.currentTimeMillis()
)

class Converters {
    @TypeConverter
    fun fromListToString(list: List<String>): String {
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    @TypeConverter
    fun fromStringToList(value: String): List<String> {
        val list = mutableListOf<String>()
        if (value.isBlank()) return list
        try {
            val jsonArray = JSONArray(value)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}

fun QuestionEntity.toQuestion(): Question {
    return Question(
        questionText = this.questionText,
        options = this.options,
        correctAnswer = this.correctAnswer
    )
}

fun Question.toEntity(): QuestionEntity {
    return QuestionEntity(
        questionText = this.questionText,
        options = this.options,
        correctAnswer = this.correctAnswer
    )
}
