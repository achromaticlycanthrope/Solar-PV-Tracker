package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SolarRecord::class], version = 1, exportSchema = false)
abstract class SolarDatabase : RoomDatabase() {
    abstract fun solarDao(): SolarDao

    companion object {
        @Volatile
        private var INSTANCE: SolarDatabase? = null

        fun getDatabase(context: Context): SolarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SolarDatabase::class.java,
                    "solar_pv_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
