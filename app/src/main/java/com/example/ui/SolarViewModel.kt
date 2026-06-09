package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class SolarViewModel(application: Application) : AndroidViewModel(application) {
    private val database = SolarDatabase.getDatabase(application)
    private val repository = SolarRepository(database.solarDao())

    // All records reactive stream
    val allRecords = repository.allRecordsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI Configuration State
    private val _selectedTimeframe = MutableStateFlow(Timeframe.MONTH_COMPARISON) // YEARLY, MONTH_COMPARISON, DAILY
    val selectedTimeframe = _selectedTimeframe.asStateFlow()

    private val _selectedYear = MutableStateFlow("2026")
    val selectedYear = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow("05") // MM format
    val selectedMonth = _selectedMonth.asStateFlow()

    // CSV Input State
    private val _csvInputText = MutableStateFlow("")
    val csvInputText = _csvInputText.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState = _importState.asStateFlow()

    // Interactive Chart Selection
    private val _selectedChartIndex = MutableStateFlow<Int?>(null)
    val selectedChartIndex = _selectedChartIndex.asStateFlow()

    init {
        // Auto-populate with simulated historical data on the first launch if the database is empty
        viewModelScope.launch {
            allRecords.first { it.isNotEmpty() || true } // wait for initial fetch
            if (repository.getAllRecords().isEmpty()) {
                loadPrepopulatedDatabase()
            }
        }
    }

    enum class Timeframe {
        YEAR_OVER_YEAR, MONTH_COMPARISON, DAILY
    }

    sealed interface ImportState {
        object Idle : ImportState
        object Parsing : ImportState
        data class Error(val message: String) : ImportState
        data class Preview(val records: List<SolarRecord>) : ImportState
        data class Success(val count: Int) : ImportState
    }

    fun setTimeframe(timeframe: Timeframe) {
        _selectedTimeframe.value = timeframe
        _selectedChartIndex.value = null
    }

    fun setSelectedYear(year: String) {
        _selectedYear.value = year
        _selectedChartIndex.value = null
    }

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
        _selectedChartIndex.value = null
    }

    fun setCsvInputText(text: String) {
        _csvInputText.value = text
        if (_importState.value is ImportState.Success || _importState.value is ImportState.Error) {
            _importState.value = ImportState.Idle
        }
    }

    fun setSelectedChartIndex(index: Int?) {
        _selectedChartIndex.value = index
    }

    // Action: Parse CSV pasted text
    fun parsePastedCsv() {
        val text = _csvInputText.value
        if (text.isBlank()) {
            _importState.value = ImportState.Error("Please paste some CSV data first.")
            return
        }
        _importState.value = ImportState.Parsing
        viewModelScope.launch {
            try {
                val parsed = SolarCsvParser.parseCsvContent(text)
                if (parsed.isEmpty()) {
                    _importState.value = ImportState.Error("Could not parse any valid solar records. Check column headers.")
                } else {
                    _importState.value = ImportState.Preview(parsed)
                }
            } catch (e: Exception) {
                _importState.value = ImportState.Error("Parsing failed: ${e.localizedMessage}")
            }
        }
    }

    // Action: Commit parsed preview to Local Master Database
    fun commitImport(records: List<SolarRecord>) {
        viewModelScope.launch {
            repository.insertRecords(records)
            _importState.value = ImportState.Success(records.size)
            _csvInputText.value = ""
        }
    }

    // Action: Cancel current import preview
    fun cancelImport() {
        _importState.value = ImportState.Idle
    }

    // Action: Add / Edit a single record manually
    fun upsertSingleRecord(date: String, myEnergy: Double, cityAverageEnergy: Double?, cityName: String = "Mumbai") {
        viewModelScope.launch {
            repository.insertRecord(SolarRecord(date, myEnergy, cityAverageEnergy, cityName))
        }
    }

    // Action: Delete a single record by index
    fun deleteRecord(date: String) {
        viewModelScope.launch {
            repository.deleteRecordByDate(date)
        }
    }

    // Action: Clear whole DB
    fun clearDatabase() {
        viewModelScope.launch {
            repository.clearAllRecords()
            _selectedChartIndex.value = null
        }
    }

    // Action: Seed with default 2019-2026 data
    fun loadPrepopulatedDatabase() {
        viewModelScope.launch {
            _importState.value = ImportState.Parsing
            val data = SolarDataGenerator.generateHistoricalData()
            repository.clearAllRecords()
            repository.insertRecords(data)
            _importState.value = ImportState.Success(data.size)
        }
    }

    // State Calculations
    // 1. Get List of Years in local Master DB
    val availableYears = allRecords.map { records ->
        records.map { it.date.take(4) }.distinct().sortedDescending()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("2026"))

    // 2. Yearly Data Aggregates
    val yearlyAggregates = allRecords.map { records ->
        records.groupBy { it.date.take(4) }
            .map { (year, yearRecords) ->
                val mySum = yearRecords.sumOf { it.myEnergy }
                val citiesRaw = yearRecords.mapNotNull { it.cityAverageEnergy }
                val avgSum = if (citiesRaw.isNotEmpty()) {
                    yearRecords.sumOf { record -> record.cityAverageEnergy ?: record.myEnergy }
                } else {
                    0.0
                }
                YearlyAggregate(
                    year = year,
                    myTotal = Math.round(mySum * 10.0) / 10.0,
                    cityTotal = Math.round(avgSum * 10.0) / 10.0,
                    recordCount = yearRecords.size
                )
            }.sortedBy { it.year }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Monthly Aggregates for dynamic selectedYear
    val monthlyAggregates = combine(allRecords, _selectedYear) { records, year ->
        val yearRecords = records.filter { it.date.startsWith(year) }
        (1..12).map { m ->
            val monthStr = String.format("%02d", m)
            val monthRecords = yearRecords.filter { it.date.substring(5, 7) == monthStr }
            val mySum = monthRecords.sumOf { it.myEnergy }
            val avgSum = monthRecords.sumOf { it.cityAverageEnergy ?: 0.0 }
            val name = when (m) {
                1 -> "Jan"
                2 -> "Feb"
                3 -> "Mar"
                4 -> "Apr"
                5 -> "May"
                6 -> "Jun"
                7 -> "Jul"
                8 -> "Aug"
                9 -> "Sep"
                10 -> "Oct"
                11 -> "Nov"
                12 -> "Dec"
                else -> ""
            }
            MonthlyAggregate(
                monthCode = monthStr,
                monthName = name,
                myTotal = Math.round(mySum * 10.0) / 10.0,
                cityTotal = Math.round(avgSum * 10.0) / 10.0,
                daysCount = monthRecords.size
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Daily detailed records for selectedYear & selectedMonth
    val dailyRecords = combine(allRecords, _selectedYear, _selectedMonth) { records, year, month ->
        records.filter { it.date.startsWith("$year-$month") }
            .sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

// Data models for analytical visualizations
data class YearlyAggregate(
    val year: String,
    val myTotal: Double,
    val cityTotal: Double,
    val recordCount: Int
)

data class MonthlyAggregate(
    val monthCode: String,
    val monthName: String,
    val myTotal: Double,
    val cityTotal: Double,
    val daysCount: Int
)
