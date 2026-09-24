package com.example.preforge.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ExamEntity::class, QuestionEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao

    abstract fun questionDao(): QuestionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateQuestionBank(
                    database = db,
                    sourceTable = "questions",
                    questionColumn = "questionText",
                    optionsColumn = "options",
                    answerColumn = "correctAnswer",
                    createdAtColumn = "createdAt"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateQuestionBank(
                    database = db,
                    sourceTable = "preguntas",
                    questionColumn = "texto_pregunta",
                    optionsColumn = "opciones",
                    answerColumn = "respuesta_correcta",
                    createdAtColumn = "fecha_creacion"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "preforge_database"
                )
                    .addMigrations(MIGRATION_1_3, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun migrateQuestionBank(
            database: SupportSQLiteDatabase,
            sourceTable: String,
            questionColumn: String,
            optionsColumn: String,
            answerColumn: String,
            createdAtColumn: String
        ) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS exams (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    user_id TEXT NOT NULL,
                    owner_name TEXT NOT NULL,
                    source_file_name TEXT,
                    question_count INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_exams_user_id ON exams(user_id)"
            )
            database.execSQL(
                """
                INSERT INTO exams (
                    title,
                    user_id,
                    owner_name,
                    source_file_name,
                    question_count,
                    created_at
                )
                SELECT
                    'Preguntas importadas',
                    'legacy',
                    'Preguntas anteriores',
                    NULL,
                    COUNT(*),
                    COALESCE(MIN($createdAtColumn), 0)
                FROM $sourceTable
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE TABLE preguntas_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    texto_pregunta TEXT NOT NULL,
                    opciones TEXT NOT NULL,
                    respuesta_correcta TEXT NOT NULL,
                    fecha_creacion INTEGER NOT NULL,
                    exam_id INTEGER NOT NULL,
                    FOREIGN KEY(exam_id) REFERENCES exams(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO preguntas_new (
                    id,
                    texto_pregunta,
                    opciones,
                    respuesta_correcta,
                    fecha_creacion,
                    exam_id
                )
                SELECT
                    id,
                    $questionColumn,
                    $optionsColumn,
                    $answerColumn,
                    $createdAtColumn,
                    (SELECT id FROM exams WHERE user_id = 'legacy' ORDER BY id LIMIT 1)
                FROM $sourceTable
                """.trimIndent()
            )
            database.execSQL("DROP TABLE $sourceTable")
            database.execSQL("ALTER TABLE preguntas_new RENAME TO preguntas")
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_preguntas_exam_id ON preguntas(exam_id)"
            )
        }
    }
}
