package com.example.preforge.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.preforge.Question
import org.json.JSONArray

@Entity(
    tableName = "preguntas",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["exam_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["exam_id"])]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "texto_pregunta")
    val questionText: String,

    @ColumnInfo(name = "opciones")
    val options: List<String>,

    @ColumnInfo(name = "respuesta_correcta")
    val correctAnswer: String,

    @ColumnInfo(name = "fecha_creacion")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "exam_id")
    val examId: Int = 0
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

        return try {
            val jsonArray = JSONArray(value)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (exception: Exception) {
            list
        }
    }
}

fun QuestionEntity.toQuestion(): Question {
    return Question(
        questionText = questionText,
        options = options,
        correctAnswer = correctAnswer
    )
}

fun Question.toEntity(examId: Int = 0): QuestionEntity {
    return QuestionEntity(
        questionText = questionText,
        options = options,
        correctAnswer = correctAnswer,
        examId = examId
    )
}
