package com.perez.dutchlearner.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow


// Entidad para guardar frases
@Entity(tableName = "phrases")
data class PhraseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "spanish_text")
    val spanishText: String,

    @ColumnInfo(name = "dutch_text")
    val dutchText: String,

    @ColumnInfo(name = "unknown_words_count")
    val unknownWordsCount: Int = 0,

    @ColumnInfo(name = "unknown_words")
    val unknownWords: String = "", // Separado por comas

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "times_reviewed")
    val timesReviewed: Int = 0,

    @ColumnInfo(name = "last_reviewed")
    val lastReviewed: Long? = null
)

// DAO para operaciones de base de datos
@Dao
interface PhraseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: PhraseEntity): Long

    @Update
    suspend fun updatePhrase(phrase: PhraseEntity)

    @Delete
    suspend fun deletePhrase(phrase: PhraseEntity)

    @Query("SELECT * FROM phrases ORDER BY unknown_words_count DESC, created_at DESC")
    fun getAllPhrasesByRanking(): Flow<List<PhraseEntity>>

    @Query("SELECT * FROM phrases ORDER BY created_at DESC")
    fun getAllPhrasesByDate(): Flow<List<PhraseEntity>>

    @Query("SELECT * FROM phrases WHERE id = :id")
    suspend fun getPhraseById(id: Long): PhraseEntity?

    @Query("SELECT * FROM phrases ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomPhrase(): PhraseEntity?

    @Query("SELECT COUNT(*) FROM phrases")
    suspend fun getPhrasesCount(): Int

    @Query("""
        UPDATE phrases 
        SET times_reviewed = times_reviewed + 1, 
            last_reviewed = :timestamp 
        WHERE id = :id
    """)
    suspend fun markAsReviewed(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM phrases ORDER BY created_at DESC")
    fun getAllPhrasesSync(): List<PhraseEntity>
}

// Base de datos
@Database(
    entities = [PhraseEntity::class, UnknownWordEntity::class, AlarmEntity::class],
    version = 3, // ← Incrementar versión
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun phraseDao(): PhraseDao
    abstract fun unknownWordDao(): UnknownWordDao
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dutch_learner_database"
                )
                    .fallbackToDestructiveMigration() // ← Importante para cambio de schema
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Entidad para palabras DESCONOCIDAS (cambio principal)
@Entity(tableName = "unknown_words")
data class UnknownWordEntity(
    @PrimaryKey
    val word: String,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "times_seen")
    val timesSeen: Int = 1,

    @ColumnInfo(name = "learned")
    val learned: Boolean = false, // ← NUEVO: marcar cuando se aprende

    @ColumnInfo(name = "difficulty")
    val difficulty: Int = 0 // ← NUEVO: 0=fácil, 1=media, 2=difícil
)

@Dao
interface UnknownWordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: UnknownWordEntity)

    @Query("SELECT * FROM unknown_words WHERE word = :word")
    suspend fun getWord(word: String): UnknownWordEntity?

    @Query("SELECT * FROM unknown_words WHERE learned = 0 ORDER BY times_seen DESC, word ASC")
    fun getAllUnknownWords(): Flow<List<UnknownWordEntity>>

    @Query("SELECT * FROM unknown_words WHERE learned = 1 ORDER BY word ASC")
    fun getAllLearnedWords(): Flow<List<UnknownWordEntity>>

    @Query("SELECT COUNT(*) FROM unknown_words WHERE learned = 0")
    suspend fun getUnknownWordsCount(): Int

    @Query("SELECT COUNT(*) FROM unknown_words WHERE learned = 1")
    suspend fun getLearnedWordsCount(): Int

    @Query("UPDATE unknown_words SET times_seen = times_seen + 1 WHERE word = :word")
    suspend fun incrementWordSeen(word: String)

    @Query("UPDATE unknown_words SET learned = 1 WHERE word = :word")
    suspend fun markAsLearned(word: String)

    @Query("UPDATE unknown_words SET learned = 0 WHERE word = :word")
    suspend fun markAsUnknown(word: String)

    @Delete
    suspend fun deleteWord(word: UnknownWordEntity)

    @Query("DELETE FROM unknown_words WHERE learned = 1")
    suspend fun deleteAllLearned()
}