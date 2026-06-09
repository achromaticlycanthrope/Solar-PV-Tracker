package com.example.data

import kotlinx.coroutines.flow.Flow

class SolarRepository(private val solarDao: SolarDao) {
    val allRecordsFlow: Flow<List<SolarRecord>> = solarDao.getAllRecordsFlow()

    suspend fun getAllRecords(): List<SolarRecord> {
        return solarDao.getAllRecords()
    }

    suspend fun insertRecords(records: List<SolarRecord>) {
        solarDao.insertRecords(records)
    }

    suspend fun insertRecord(record: SolarRecord) {
        solarDao.insertRecord(record)
    }

    suspend fun deleteRecordByDate(date: String) {
        solarDao.deleteRecordByDate(date)
    }

    suspend fun clearAllRecords() {
        solarDao.clearAllRecords()
    }
}
