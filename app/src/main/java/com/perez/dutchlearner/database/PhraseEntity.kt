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
    val unknownWords: String = "", // JSON o separado por comas

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
}

// Base de datos
@Database(
    entities = [PhraseEntity::class, KnownWordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun phraseDao(): PhraseDao
    abstract fun knownWordDao(): KnownWordDao

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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Entidad para palabras conocidas
@Entity(tableName = "known_words")
data class KnownWordEntity(
    @PrimaryKey
    val word: String,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "times_seen")
    val timesSeen: Int = 1
)

@Dao
interface KnownWordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: KnownWordEntity)

    @Query("SELECT * FROM known_words WHERE word = :word")
    suspend fun isWordKnown(word: String): KnownWordEntity?

    @Query("SELECT * FROM known_words ORDER BY word ASC")
    fun getAllKnownWords(): Flow<List<KnownWordEntity>>

    @Query("SELECT COUNT(*) FROM known_words")
    suspend fun getKnownWordsCount(): Int

    @Query("UPDATE known_words SET times_seen = times_seen + 1 WHERE word = :word")
    suspend fun incrementWordSeen(word: String)

    @Delete
    suspend fun deleteWord(word: KnownWordEntity)
}