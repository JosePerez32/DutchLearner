package com.perez.dutchlearner.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.perez.dutchlearner.database.PhraseEntity

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "hour")
    val hour: Int,

    @ColumnInfo(name = "minute")
    val minute: Int,

    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,

    @ColumnInfo(name = "days_of_week")
    val daysOfWeek: String = "1,2,3,4,5,6,7", // Lunes=1, Domingo=7

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getTimeString(): String = String.format("%02d:%02d", hour, minute)

    fun getDaysActive(): List<Int> =
        daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }
}

@Dao
interface AlarmDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE enabled = 1 ORDER BY hour ASC, minute ASC")
    fun getEnabledAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setAlarmEnabled(id: Long, enabled: Boolean)

    @Query("SELECT COUNT(*) FROM alarms")
    suspend fun getAlarmsCount(): Int

    @Query("DELETE FROM alarms")
    suspend fun deleteAllAlarms()
}