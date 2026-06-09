package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.window.Dialog
import com.example.data.SolarRecord
import com.example.ui.theme.CityCoral
import com.example.ui.theme.PlantEmerald
import com.example.ui.theme.SolarGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolarDashboardScreen(
    viewModel: SolarViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Charts, 1: Import, 2: Ledger
    val records by viewModel.allRecords.collectAsState()

    // Dialog state for manually adding logs
    var showManualAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Solar PV Tracker",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Local Master: ${records.size} historical days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showManualAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Manual Entry")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Material 3 bottom navigation tabs
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Text("📊", fontSize = 20.sp) },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Text("📥", fontSize = 20.sp) },
                    label = { Text("CSV Import") }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Text("📋", fontSize = 20.sp) },
                    label = { Text("Master Ledger") }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                0 -> DashboardTab(viewModel)
                1 -> ImportTab(viewModel)
                2 -> LedgerTab(viewModel, onOpenAddDialog = { showManualAddDialog = true })
            }
        }
    }

    if (showManualAddDialog) {
        ManualAddRecordDialog(
            onDismiss = { showManualAddDialog = false },
            onSave = { date, myVal, avgVal ->
                viewModel.upsertSingleRecord(date, myVal, avgVal)
                showManualAddDialog = false
            }
        )
    }
}

@Composable
fun DashboardTab(viewModel: SolarViewModel) {
    val records by viewModel.allRecords.collectAsState()
    val timeframe by viewModel.selectedTimeframe.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val availableYears by viewModel.availableYears.collectAsState()
    
    val selectedChartIndex by viewModel.selectedChartIndex.collectAsState()

    val yearlyAggs by viewModel.yearlyAggregates.collectAsState()
    val monthlyAggs by viewModel.monthlyAggregates.collectAsState()
    val dailyRecords by viewModel.dailyRecords.collectAsState()

    var showYearDropdown by remember { mutableStateOf(false) }
    var showMonthDropdown by remember { mutableStateOf(false) }

    val monthsList = listOf(
        "01" to "January", "02" to "February", "03" to "March", "04" to "April",
        "05" to "May", "06" to "June", "07" to "July", "08" to "August",
        "09" to "September", "10" to "October", "11" to "November", "12" to "December"
    )

    // Calculate High Density production comparison totals for active timeframe
    val activeMyTotal: Double
    val activeCityTotal: Double
    val activeTitleLabel: String

    when (timeframe) {
        SolarViewModel.Timeframe.YEAR_OVER_YEAR -> {
            activeMyTotal = yearlyAggs.sumOf { it.myTotal }
            activeCityTotal = yearlyAggs.sumOf { it.cityTotal }
            activeTitleLabel = "Overall Historical Production"
        }
        SolarViewModel.Timeframe.MONTH_COMPARISON -> {
            activeMyTotal = monthlyAggs.sumOf { it.myTotal }
            activeCityTotal = monthlyAggs.sumOf { it.cityTotal }
            activeTitleLabel = "Year $selectedYear Production"
        }
        SolarViewModel.Timeframe.DAILY -> {
            activeMyTotal = dailyRecords.sumOf { it.myEnergy }
            activeCityTotal = dailyRecords.sumOf { it.cityAverageEnergy ?: 0.0 }
            val activeMonthName = monthsList.find { it.first == selectedMonth }?.second ?: selectedMonth
            activeTitleLabel = "$activeMonthName $selectedYear Production"
        }
    }

    val compDiff = activeMyTotal - activeCityTotal
    val compPercent = if (activeCityTotal > 0) (compDiff / activeCityTotal) * 100 else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Timeframe Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Historical Trend Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Compare your local clean production with city wide residential expectations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Timeframe pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SolarViewModel.Timeframe.values().forEach { tf ->
                            val isSelected = timeframe == tf
                            val label = when (tf) {
                                SolarViewModel.Timeframe.YEAR_OVER_YEAR -> "Yearly (Agg)"
                                SolarViewModel.Timeframe.MONTH_COMPARISON -> "Monthly Details"
                                SolarViewModel.Timeframe.DAILY -> "Daily Breakdown"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setTimeframe(tf) },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // High Density Comparative Production Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = activeTitleLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "${String.format(Locale.US, "%,.1f", activeMyTotal)} kWh",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF21005D), // Classic Deep purple contrast typography from custom theme
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Comparison pill
                        val isSurplus = activeMyTotal >= activeCityTotal
                        val pillBgColor = if (isSurplus) Color(0xFFE8DEF8) else Color(0xFFFAC8C8)
                        val pillTextColor = if (isSurplus) Color(0xFF1D192B) else Color(0xFF811d1d)
                        val trendIcon = if (isSurplus) "↗" else "↘"
                        val prefix = if (isSurplus) "+" else ""

                        Row(
                            modifier = Modifier
                                .background(pillBgColor, shape = RoundedCornerShape(100.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(trendIcon, fontSize = 11.sp)
                            Text(
                                text = "$prefix${String.format(Locale.US, "%.1f", compPercent)}% vs City",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = pillTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val maxVal = maxOf(activeMyTotal, activeCityTotal).coerceAtLeast(1.0)
                    val myFraction = (activeMyTotal / maxVal).toFloat()
                    val cityFraction = (activeCityTotal / maxVal).toFloat()

                    // Bar 1: Your System
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Your System",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${String.format(Locale.US, "%,.1f", activeMyTotal)} kWh",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = PlantEmerald
                            )
                        }

                        // Progress line container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(100.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(myFraction)
                                    .fillMaxHeight()
                                    .background(
                                        PlantEmerald,
                                        shape = RoundedCornerShape(100.dp)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bar 2: City Average
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "City Average (Mumbai)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Text(
                                "${String.format(Locale.US, "%,.1f", activeCityTotal)} kWh",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = CityCoral
                            )
                        }

                        // Progress line container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(100.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(cityFraction)
                                    .fillMaxHeight()
                                    .background(
                                        CityCoral,
                                        shape = RoundedCornerShape(100.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Dropdown Selectors
        if (timeframe != SolarViewModel.Timeframe.YEAR_OVER_YEAR) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Year Picker
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showYearDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Year: $selectedYear ▾", fontWeight = FontWeight.SemiBold)
                        }
                        DropdownMenu(
                            expanded = showYearDropdown,
                            onDismissRequest = { showYearDropdown = false }
                        ) {
                            availableYears.forEach { yr ->
                                DropdownMenuItem(
                                    text = { Text(yr) },
                                    onClick = {
                                        viewModel.setSelectedYear(yr)
                                        showYearDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Month Picker
                    if (timeframe == SolarViewModel.Timeframe.DAILY) {
                        Box(modifier = Modifier.weight(1f)) {
                            val activeMonthName = monthsList.find { it.first == selectedMonth }?.second ?: selectedMonth
                            OutlinedButton(
                                onClick = { showMonthDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Month: max($activeMonthName) ▾".replace("max(", "").replace(")", ""), fontWeight = FontWeight.SemiBold)
                            }
                            DropdownMenu(
                                expanded = showMonthDropdown,
                                onDismissRequest = { showMonthDropdown = false }
                            ) {
                                monthsList.forEach { (code, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            viewModel.setSelectedMonth(code)
                                            showMonthDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Chart Visualization
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header title for chart
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (timeframe) {
                                SolarViewModel.Timeframe.YEAR_OVER_YEAR -> "Yearly Energy comparison (since 2019)"
                                SolarViewModel.Timeframe.MONTH_COMPARISON -> "Monthly comparison for Year $selectedYear"
                                SolarViewModel.Timeframe.DAILY -> "Daily logs: ${monthsList.find { it.first == selectedMonth }?.second} $selectedYear"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Legend
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(PlantEmerald)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("My Production", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(CityCoral)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mumbai City Avg", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Render Chart
                    when (timeframe) {
                        SolarViewModel.Timeframe.YEAR_OVER_YEAR -> {
                            YearlyComparisonChart(
                                aggregates = yearlyAggs,
                                selectedIndex = selectedChartIndex,
                                onIndexSelected = { viewModel.setSelectedChartIndex(it) }
                            )
                        }
                        SolarViewModel.Timeframe.MONTH_COMPARISON -> {
                            MonthlyComparisonChart(
                                aggregates = monthlyAggs,
                                selectedIndex = selectedChartIndex,
                                onIndexSelected = { viewModel.setSelectedChartIndex(it) }
                            )
                        }
                        SolarViewModel.Timeframe.DAILY -> {
                            DailyComparisonChart(
                                records = dailyRecords,
                                selectedIndex = selectedChartIndex,
                                onIndexSelected = { viewModel.setSelectedChartIndex(it) }
                            )
                        }
                    }
                }
            }
        }

        // Key Analytical Stat metrics
        item {
            // Compute cumulative differences
            val myTotalSum: Double
            val cityTotalSum: Double
            val count: Int
            val labelText: String

            when (timeframe) {
                SolarViewModel.Timeframe.YEAR_OVER_YEAR -> {
                    myTotalSum = yearlyAggs.sumOf { it.myTotal }
                    cityTotalSum = yearlyAggs.sumOf { it.cityTotal }
                    count = yearlyAggs.size
                    labelText = "Overall Historical Summary"
                }
                SolarViewModel.Timeframe.MONTH_COMPARISON -> {
                    myTotalSum = monthlyAggs.sumOf { it.myTotal }
                    cityTotalSum = monthlyAggs.sumOf { it.cityTotal }
                    count = monthlyAggs.filter { it.daysCount > 0 }.size
                    labelText = "Total Summary of Year $selectedYear"
                }
                SolarViewModel.Timeframe.DAILY -> {
                    myTotalSum = dailyRecords.sumOf { it.myEnergy }
                    cityTotalSum = dailyRecords.sumOf { it.cityAverageEnergy ?: 0.0 }
                    count = dailyRecords.size
                    labelText = "Daily Summary of Selected Month"
                }
            }

            val diff = myTotalSum - cityTotalSum
            val ratio = if (cityTotalSum > 0) (diff / cityTotalSum) * 100 else 0.0
            
            Column {
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Total Generation", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(
                                "${String.format("%,.1f", myTotalSum)} kWh",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlantEmerald
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("From sun directly", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("City Average Base", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(
                                "${String.format("%,.1f", cityTotalSum)} kWh",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CityCoral
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Average customers", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Comparative Surplus & Environmental offsets card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (diff >= 0) {
                            PlantEmerald.copy(alpha = 0.08f)
                        } else {
                            CityCoral.copy(alpha = 0.08f)
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (diff >= 0) "🌳" else "⚠️",
                            fontSize = 32.sp,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (diff >= 0) "Surplus Output Performance" else "Production deficit",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (diff >= 0) PlantEmerald else CityCoral
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (diff >= 0) {
                                    "Your solar system generated ${String.format("%,.1f", diff)} kWh more than Mumbai's average customer baseline (a surplus density of +${String.format("%.1f", ratio)}%)."
                                } else {
                                    "You generated ${String.format("%,.1f", -diff)} kWh less than typical city expectations due to local cloud covers or shade (deficit of ${String.format("%.1f", ratio)}%)."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (diff >= 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                // CO2 Offset calc: general average is 0.85 kg of CO2 equivalent per kWh saved
                                val co2Saved = myTotalSum * 0.82
                                val treesEquivalent = co2Saved / 21.0 // 1 mature tree absorbs ~21kg of CO2 per year
                                Text(
                                    text = "⚡ Carbon offset: ${String.format("%,.1f", co2Saved)} kg of greenhouse gas CO₂ offset (equivalent to preserving ~${Math.round(treesEquivalent)} trees!).",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PlantEmerald
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportTab(viewModel: SolarViewModel) {
    val csvText by viewModel.csvInputText.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Sample data to demo
    val sampleCsv = """"DateTime","My Solar Energy","Avg Mumbai Energy"
"2026-05-01 00:00:00",24,25
"2026-05-02 00:00:00",24,25
"2026-05-03 00:00:00",23,26
"2026-05-04 00:00:00",23
"2026-05-05 00:00:00",21,26
"2026-05-06 00:00:00",22,26
"2026-05-07 00:00:00",21,25
"2026-05-08 00:00:00",20,24
"2026-05-09 00:00:00",9,25
"2026-05-10 00:00:00",20,25
"2026-05-11 00:00:00",20,25
"2026-05-12 00:00:00",20,26
"2026-05-13 00:00:00",19,23
"2026-05-14 00:00:00",28,25
"2026-05-15 00:00:00",27,24
"2026-05-16 00:00:00",6,25
"2026-05-17 00:00:00",22,25
"2026-05-18 00:00:00",21
"2026-05-19 00:00:00",19,22
"2026-05-20 00:00:00",17,23
"2026-05-21 00:00:00",20,23
"2026-05-22 00:00:00",16,19
"2026-05-23 00:00:00",18,21
"2026-05-24 00:00:00",21,22
"2026-05-25 00:00:00",21,24
"2026-05-26 00:00:00",20,24
"2026-05-27 00:00:00",19,25
"2026-05-28 00:00:00",21,25
"2026-05-29 00:00:00",21,23
"2026-05-30 00:00:00",20,23
"2026-05-31 00:00:00",22,26"""

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Automate Log Processing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Paste the generated monthly CSV file directly here. This app parses strings, aligns dates, computes city comparisons, and merges them smoothly with your 2019-2026 master record.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        when (val state = importState) {
            is SolarViewModel.ImportState.Idle, is SolarViewModel.ImportState.Error -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Paste CSV Data String", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            
                            // Load Demo Data button
                            TextButton(
                                onClick = {
                                    viewModel.setCsvInputText(sampleCsv)
                                }
                            ) {
                                Text("💡 Load Mumbai Demo", fontSize = 12.sp)
                            }
                        }

                        OutlinedTextField(
                            value = csvText,
                            onValueChange = { viewModel.setCsvInputText(it) },
                            placeholder = { Text("example:\n\"DateTime\",\"My Solar Energy\",\"Avg Mumbai Energy\"\n\"2026-05-01 00:00:00\",24,25") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            textStyle = TextStyle(fontSize = 13.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (state is SolarViewModel.ImportState.Error) {
                            Text(
                                text = "❌ ${state.message}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.parsePastedCsv()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PlantEmerald)
                        ) {
                            Text("Parse CSV Data Stream", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is SolarViewModel.ImportState.Parsing -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PlantEmerald)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Analyzing parameters and resolving header mapping...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            is SolarViewModel.ImportState.Preview -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📊 CSV Import Map Preview", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "We successfully matched columns and identified city as: ${state.records.firstOrNull()?.cityName ?: "Mumbai"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PlantEmerald
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Detected Logs:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("${state.records.size} days", style = MaterialTheme.typography.bodySmall)
                                }
                                val firstVal = state.records.firstOrNull()
                                val lastVal = state.records.lastOrNull()
                                if (firstVal != null && lastVal != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Date Range:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text("${firstVal.date} to ${lastVal.date}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Avg Generation:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("${String.format("%.1f", state.records.map { it.myEnergy }.average())} kWh", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.cancelImport() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = { viewModel.commitImport(state.records) },
                                    modifier = Modifier.weight(1.5f),
                                    colors = ButtonDefaults.buttonColors(containerColor = PlantEmerald),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Merge & Update DB", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            is SolarViewModel.ImportState.Success -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PlantEmerald.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("☀️", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Database Successfully Updated!",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = PlantEmerald
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Imported and merged ${state.count} daily records smoothly. Comparison trends and statistics are re-calculated ready in your primary dashboard.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.cancelImport() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PlantEmerald)
                            ) {
                                Text("Import Another Log File", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerTab(viewModel: SolarViewModel, onOpenAddDialog: () -> Unit) {
    val records by viewModel.allRecords.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    // Sort ledger by date descending for reading convenience (newest first)
    val filteredRecords = remember(records, searchQuery) {
        records.filter { record ->
            record.date.contains(searchQuery)
        }.sortedByDescending { it.date }
    }

    var showConfirmDeleteDb by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Master Database Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Manage entries, edit values manually, or populate mock datasets from 2019.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenAddDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = PlantEmerald),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Manual Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.loadPrepopulatedDatabase() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Master (2019-2026)", fontSize = 10.sp, maxLines = 1)
                    }

                    IconButton(
                        onClick = { showConfirmDeleteDb = true },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Wipe Database")
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter entries by date (e.g. 2026-05)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            }
        )

        if (filteredRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No solar entries fit this search criteria.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            Text(
                text = "Displaying ${filteredRecords.size} records matching query",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRecords, key = { it.date }) { record ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(record.date, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("City: ${record.cityName}", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("My: ${record.myEnergy} kWh", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = PlantEmerald)
                                    Text("Avg: ${record.cityAverageEnergy?.let { "$it kWh" } ?: "N/A"}", style = MaterialTheme.typography.bodySmall, color = CityCoral)
                                }

                                IconButton(
                                    onClick = { viewModel.deleteRecord(record.date) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete entry", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDeleteDb) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDb = false },
            title = { Text("Danger Zone") },
            text = { Text("Are you absolutely sure you want to delete all historical logs? This completely wipes the local master database database.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearDatabase()
                        showConfirmDeleteDb = false
                    }
                ) {
                    Text("Delete Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDb = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ManualAddRecordDialog(
    onDismiss: () -> Unit,
    onSave: (date: String, myEnergy: Double, cityAverage: Double?) -> Unit
) {
    var dateString by remember {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        mutableStateOf(today)
    }
    var myEnergyStr by remember { mutableStateOf("") }
    var cityAverageStr by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Insert Solar Log Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Date (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = myEnergyStr,
                    onValueChange = { myEnergyStr = it },
                    label = { Text("My Solar Energy (kWh)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = cityAverageStr,
                    onValueChange = { cityAverageStr = it },
                    label = { Text("Avg Mumbai Energy (kWh) - Optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                if (inputError != null) {
                    Text(
                        text = inputError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val myVal = myEnergyStr.toDoubleOrNull()
                            val avgVal = cityAverageStr.toDoubleOrNull()
                            
                            if (dateString.trim().length != 10 || !dateString.contains("-")) {
                                inputError = "Date must fit the yyyy-MM-dd format exactly."
                                return@Button
                            }
                            if (myVal == null) {
                                inputError = "Please write a valid decimal value for My Solar Energy."
                                return@Button
                            }
                            onSave(dateString, myVal, avgVal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PlantEmerald)
                    ) {
                        Text("Save Log", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
