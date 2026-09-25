package com.example.preforge.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExamEntity::class,
        QuestionEntity::class,
        ReviewCardEntity::class,
        ReviewAttemptEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao

    abstract fun questionDao(): QuestionDao

    abstract fun reviewCardDao(): ReviewCardDao

    abstract fun reviewAttemptDao(): ReviewAttemptDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS review_cards (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        question_id INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        ease_factor REAL NOT NULL,
                        interval_days INTEGER NOT NULL,
                        repetitions INTEGER NOT NULL,
                        lapses INTEGER NOT NULL,
                        last_rating INTEGER,
                        last_reviewed_at INTEGER,
                        next_review_at INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY(question_id) REFERENCES preguntas(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_review_cards_user_id_question_id
                    ON review_cards(user_id, question_id)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_review_cards_user_id_next_review_at
                    ON review_cards(user_id, next_review_at)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_review_cards_question_id
                    ON review_cards(question_id)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS review_attempts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        card_id INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        rating INTEGER NOT NULL,
                        reviewed_at INTEGER NOT NULL,
                        previous_interval_days INTEGER NOT NULL,
                        new_interval_days INTEGER NOT NULL,
                        previous_ease_factor REAL NOT NULL,
                        new_ease_factor REAL NOT NULL,
                        was_correct INTEGER NOT NULL,
                        FOREIGN KEY(card_id) REFERENCES review_cards(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_review_attempts_card_id
                    ON review_attempts(card_id)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_review_attempts_user_id_reviewed_at
                    ON review_attempts(user_id, reviewed_at)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE preguntas ADD COLUMN question_type TEXT NOT NULL DEFAULT 'MULTIPLE_CHOICE'"
                )
                db.execSQL(
                    "ALTER TABLE preguntas ADD COLUMN accepted_answers TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE preguntas ADD COLUMN explanation TEXT NOT NULL DEFAULT ''"
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
                    .addMigrations(
                        MIGRATION_1_3,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
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
