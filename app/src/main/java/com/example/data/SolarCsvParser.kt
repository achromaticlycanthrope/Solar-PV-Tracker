package com.example.data

object SolarCsvParser {
    fun parseCsvContent(csvString: String): List<SolarRecord> {
        val records = mutableListOf<SolarRecord>()
        val lines = csvString.lines()
        if (lines.isEmpty()) return emptyList()

        // Locate header line
        val headerLine = lines.firstOrNull { it.isNotBlank() && (it.contains("DateTime") || it.contains("Energy")) } ?: return emptyList()
        val headerColumns = headerLine.split(",").map { it.replace("\"", "").trim() }
        
        val dateIdx = headerColumns.indexOfFirst { it.equals("DateTime", ignoreCase = true) || it.contains("date", ignoreCase = true) }
        val myEnergyIdx = headerColumns.indexOfFirst { it.contains("My Solar", ignoreCase = true) || it.contains("my", ignoreCase = true) }
        val avgEnergyIdx = headerColumns.indexOfFirst { it.contains("Avg", ignoreCase = true) || it.contains("Average", ignoreCase = true) }
        
        // Extract City Name (e.g., "Avg Mumbai Energy" -> "Mumbai")
        var cityName = "Mumbai"
        if (avgEnergyIdx != -1) {
            val colName = headerColumns[avgEnergyIdx]
            val parts = colName.split(" ")
            val avgPartIdx = parts.indexOfFirst { it.contains("avg", ignoreCase = true) }
            val energyPartIdx = parts.indexOfFirst { it.contains("energy", ignoreCase = true) }
            if (avgPartIdx != -1 && energyPartIdx != -1 && energyPartIdx > avgPartIdx + 1) {
                cityName = parts.subList(avgPartIdx + 1, energyPartIdx).joinToString(" ")
            } else if (colName.contains("Mumbai", ignoreCase = true)) {
                cityName = "Mumbai"
            }
        }

        // Parse each actual row
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isBlank() || trimmedLine == headerLine || trimmedLine.startsWith("#")) continue
            
            val columns = trimmedLine.split(",").map { it.replace("\"", "").trim() }
            if (columns.size <= dateIdx || dateIdx == -1) continue
            
            val rawDateTime = columns[dateIdx]
            if (rawDateTime.isBlank()) continue
            
            // Extract only date part "YYYY-MM-DD" (e.g. from "2026-05-01 00:00:00" to "2026-05-01")
            val date = rawDateTime.substringBefore(" ") 
            
            val myEnergy = if (myEnergyIdx != -1 && myEnergyIdx < columns.size) {
                columns[myEnergyIdx].toDoubleOrNull() ?: 0.0
            } else {
                0.0
            }
            
            val avgEnergy = if (avgEnergyIdx != -1 && avgEnergyIdx < columns.size) {
                columns[avgEnergyIdx].toDoubleOrNull()
            } else {
                null
            }
            
            records.add(
                SolarRecord(
                    date = date,
                    myEnergy = myEnergy,
                    cityAverageEnergy = avgEnergy,
                    cityName = cityName
                )
            )
        }
        return records
    }
}
