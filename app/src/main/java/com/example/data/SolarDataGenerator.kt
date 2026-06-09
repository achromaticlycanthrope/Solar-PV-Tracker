package com.example.data

object SolarDataGenerator {
    fun generateHistoricalData(): List<SolarRecord> {
        val records = mutableListOf<SolarRecord>()
        val startYear = 2019
        val endYear = 2026
        
        // Mumbai monthly averages baseline pattern
        // High solar production in Mar/Apr/May. Dipping during monsoon months Jun/Jul/Aug
        val baseMyEnergy = mapOf(
            1 to 18.0, 2 to 21.0, 3 to 25.0, 4 to 28.0, 5 to 26.0, 6 to 15.0,
            7 to 10.0, 8 to 11.0, 9 to 14.0, 10 to 19.0, 11 to 17.0, 12 to 16.0
        )
        
        val baseCityEnergy = mapOf(
            1 to 19.5, 2 to 22.0, 3 to 26.5, 4 to 29.0, 5 to 25.5, 6 to 16.5,
            7 to 11.5, 8 to 12.0, 9 to 15.0, 10 to 20.0, 11 to 18.2, 12 to 17.5
        )

        fun getDaysInMonth(year: Int, month: Int): Int {
            return when (month) {
                1, 3, 5, 7, 8, 10, 12 -> 31
                4, 6, 9, 11 -> 30
                2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
                else -> 30
            }
        }

        for (year in startYear..endYear) {
            val maxMonth = if (year == 2026) 5 else 12 // Up to May 2026
            for (month in 1..maxMonth) {
                val days = getDaysInMonth(year, month)
                val myBase = baseMyEnergy[month] ?: 20.0
                val cityBase = baseCityEnergy[month] ?: 21.0
                for (day in 1..days) {
                    val dateString = String.format("%04d-%02d-%02d", year, month, day)
                    
                    // Generate pseudo-random organic fluctuation based on day
                    val seed = (year + month * 31 + day).toDouble()
                    val noiseMy = Math.sin(seed) * 2.8 + (Math.cos(seed * 2.3) * 1.2)
                    val noiseCity = Math.cos(seed) * 2.2 + (Math.sin(seed * 1.8) * 0.9)
                    
                    // Ensure values are physically positive
                    val myVal = (myBase + noiseMy).coerceAtLeast(3.2)
                    // If date is during the provided CSV range, keep empty/unprovided city as sample but normally show real
                    val cityVal = (cityBase + noiseCity).coerceAtLeast(4.0)

                    // May 1 to May 3 cases (the user attached CSV sample):
                    // Let's replace the last month with user's specific exact numbers to make importing feel absolutely cohesive and matching!
                    if (year == 2026 && month == 5) {
                        // We will let the user import it or we can let them see it prepopulated as part of the initial data setup!
                        // Let's keep it as is.
                    }

                    // Round to 1 decimal place
                    val finalMy = Math.round(myVal * 10.0) / 10.0
                    val finalCity = Math.round(cityVal * 10.0) / 10.0

                    records.add(
                        SolarRecord(
                            date = dateString,
                            myEnergy = finalMy,
                            cityAverageEnergy = finalCity,
                            cityName = "Mumbai"
                        )
                    )
                }
            }
        }
        return records
    }
}
