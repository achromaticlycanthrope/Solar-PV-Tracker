package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SolarDao {
    @Query("SELECT * FROM solar_records ORDER BY date ASC")
    fun getAllRecordsFlow(): Flow<List<SolarRecord>>

    @Query("SELECT * FROM solar_records ORDER BY date ASC")
    suspend fun getAllRecords(): List<SolarRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<SolarRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SolarRecord)

    @Query("DELETE FROM solar_records WHERE date = :date")
    suspend fun deleteRecordByDate(date: String)

    @Query("DELETE FROM solar_records")
    suspend fun clearAllRecords()
}
