package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "solar_records")
data class SolarRecord(
    @PrimaryKey val date: String, // format "yyyy-MM-dd"
    val myEnergy: Double, // in kWh
    val cityAverageEnergy: Double? = null, // in kWh (average of city)
    val cityName: String = "Mumbai"
)
