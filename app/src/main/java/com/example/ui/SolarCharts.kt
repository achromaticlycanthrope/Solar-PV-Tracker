package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SolarRecord
import com.example.ui.theme.CityCoral
import com.example.ui.theme.PlantEmerald
import com.example.ui.theme.SolarGold

@Composable
fun YearlyComparisonChart(
    aggregates: List<YearlyAggregate>,
    selectedIndex: Int?,
    onIndexSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (aggregates.isEmpty()) {
        EmptyChartPlaceholder(message = "No yearly data records to visualize.")
        return
    }

    val maxVal = aggregates.flatMap { listOf(it.myTotal, it.cityTotal) }.maxOrNull()?.coerceAtLeast(10.0) ?: 100.0
    val chartMax = maxVal * 1.15 // 15% padding at top

    val primaryColor = PlantEmerald
    val secondaryColor = CityCoral
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier.fillMaxWidth()) {
        Column {
            // Chart draw canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .pointerInput(aggregates) {
                        detectTapGestures { offset ->
                            val totalWidth = size.width
                            val step = totalWidth / aggregates.size
                            val index = (offset.x / step).toInt().coerceIn(0, aggregates.size - 1)
                            onIndexSelected(index)
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val paddingBottom = 40f
                val paddingTop = 20f
                val paddingLeft = 10f
                val paddingRight = 10f
                
                val usableHeight = height - paddingTop - paddingBottom
                val itemWidth = (width - paddingLeft - paddingRight) / aggregates.size

                // Draw horizontal background grid lines (4 levels)
                val gridLines = 4
                for (g in 0..gridLines) {
                    val y = paddingTop + (usableHeight / gridLines) * g
                    val value = chartMax - (chartMax / gridLines) * g
                    
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = labelColor.hashCode()
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                        drawText(
                            "${Math.round(value)} kWh",
                            paddingLeft + 15f,
                            y - 8f,
                            paint
                        )
                    }
                }

                // Render side-by-side grouped bars for each year
                aggregates.forEachIndexed { idx, agg ->
                    val xCenter = paddingLeft + idx * itemWidth + itemWidth / 2f
                    val barWidth = (itemWidth * 0.35f).coerceAtMost(32f)
                    val gap = 6f

                    // User Production bar
                    val myHeight = (agg.myTotal / chartMax) * usableHeight
                    val myY = height - paddingBottom - myHeight
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(xCenter - barWidth - gap, myY.toFloat()),
                        size = Size(barWidth, myHeight.toFloat()),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // City Production bar
                    val cityHeight = (agg.cityTotal / chartMax) * usableHeight
                    val cityY = height - paddingBottom - cityHeight
                    drawRoundRect(
                        color = secondaryColor,
                        topLeft = Offset(xCenter + gap, cityY.toFloat()),
                        size = Size(barWidth, cityHeight.toFloat()),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // Label years
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = labelColor.hashCode()
                            textSize = 30f
                            textAlign = android.graphics.Paint.Align.CENTER
                            if (idx == selectedIndex) {
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                        }
                        drawText(
                            agg.year,
                            xCenter,
                            height - 10f,
                            paint
                        )
                    }

                    // Highlight indicator line under active selection
                    if (idx == selectedIndex) {
                        drawRect(
                            color = primaryColor.copy(alpha = 0.3f),
                            topLeft = Offset(xCenter - itemWidth / 2f + 5f, paddingTop),
                            size = Size(itemWidth - 10f, usableHeight),
                            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f))
                        )
                    }
                }
            }

            // Beautiful interactive detail overlay
            if (selectedIndex != null && selectedIndex < aggregates.size) {
                val item = aggregates[selectedIndex]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Year ${item.year} summary (${item.recordCount} operational days)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Your Generated Solar:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${item.myTotal} kWh",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = PlantEmerald
                            )
                        }
                        Column {
                            Text(
                                text = "Mumbai City Avg Total:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${item.cityTotal} kWh",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = CityCoral
                            )
                        }
                    }

                    val diff = item.myTotal - item.cityTotal
                    val percent = if (item.cityTotal > 0) (diff / item.cityTotal) * 100 else 0.0
                    val isPositive = diff >= 0
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isPositive) {
                            "☀️ Output comparison: +${String.format("%.1f", percent)}% surplus (${String.format("%.1f", diff)} kWh above Mumbai general public)."
                        } else {
                            "☁️ Output comparison: ${String.format("%.1f", percent)}% deficit (${String.format("%.1f", -diff)} kWh below city baseline)."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPositive) PlantEmerald else CityCoral
                    )
                }
            } else {
                Text(
                    text = "Tap on any year to analyze comparison performance",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                )
            }
        }
    }
}

@Composable
fun MonthlyComparisonChart(
    aggregates: List<MonthlyAggregate>,
    selectedIndex: Int?,
    onIndexSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (aggregates.isEmpty() || aggregates.all { it.myTotal == 0.0 && it.cityTotal == 0.0 }) {
        EmptyChartPlaceholder(message = "No monthly data records to visualize for this year.")
        return
    }

    val maxVal = aggregates.flatMap { listOf(it.myTotal, it.cityTotal) }.maxOrNull()?.coerceAtLeast(10.0) ?: 100.0
    val chartMax = maxVal * 1.15

    val primaryColor = PlantEmerald
    val secondaryColor = CityCoral
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier.fillMaxWidth()) {
        Column {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .pointerInput(aggregates) {
                        detectTapGestures { offset ->
                            val totalWidth = size.width
                            val step = totalWidth / aggregates.size
                            val index = (offset.x / step).toInt().coerceIn(0, aggregates.size - 1)
                            onIndexSelected(index)
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val paddingBottom = 40f
                val paddingTop = 20f
                val paddingLeft = 10f
                val paddingRight = 10f
                
                val usableHeight = height - paddingTop - paddingBottom
                val itemWidth = (width - paddingLeft - paddingRight) / aggregates.size

                // 4 Grid Lines
                val gridLines = 4
                for (g in 0..gridLines) {
                    val y = paddingTop + (usableHeight / gridLines) * g
                    val value = chartMax - (chartMax / gridLines) * g
                    
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = labelColor.hashCode()
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                        drawText(
                            "${Math.round(value)} kWh",
                            paddingLeft + 15f,
                            y - 8f,
                            paint
                        )
                    }
                }

                // Render 12 months bars
                aggregates.forEachIndexed { idx, agg ->
                    val xCenter = paddingLeft + idx * itemWidth + itemWidth / 2f
                    val barWidth = (itemWidth * 0.3f).coerceAtMost(16f)
                    val gap = 3f

                    // User
                    if (agg.myTotal > 0.1) {
                        val myHeight = (agg.myTotal / chartMax) * usableHeight
                        val myY = height - paddingBottom - myHeight
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(xCenter - barWidth - gap, myY.toFloat()),
                            size = Size(barWidth, myHeight.toFloat()),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }

                    // City
                    if (agg.cityTotal > 0.1) {
                        val cityHeight = (agg.cityTotal / chartMax) * usableHeight
                        val cityY = height - paddingBottom - cityHeight
                        drawRoundRect(
                            color = secondaryColor,
                            topLeft = Offset(xCenter + gap, cityY.toFloat()),
                            size = Size(barWidth, cityHeight.toFloat()),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }

                    // Month Name (draw letters)
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = labelColor.hashCode()
                            textSize = 25f
                            textAlign = android.graphics.Paint.Align.CENTER
                            if (idx == selectedIndex) {
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                        }
                        drawText(
                            agg.monthName.take(3),
                            xCenter,
                            height - 10f,
                            paint
                        )
                    }

                    // Highlight line
                    if (idx == selectedIndex) {
                        drawRect(
                            color = primaryColor.copy(alpha = 0.2f),
                            topLeft = Offset(xCenter - itemWidth / 2f + 3f, paddingTop),
                            size = Size(itemWidth - 6f, usableHeight),
                            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f))
                        )
                    }
                }
            }

            // Beautiful interactive detail overlay
            if (selectedIndex != null && selectedIndex < aggregates.size) {
                val item = aggregates[selectedIndex]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Month ${item.monthName} summary (${item.daysCount} active daily logs)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Your Generated Solar:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${item.myTotal} kWh",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = PlantEmerald
                            )
                        }
                        Column {
                            Text(
                                text = "Mumbai City Avg Total:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${item.cityTotal} kWh",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = CityCoral
                            )
                        }
                    }

                    val diff = item.myTotal - item.cityTotal
                    val percent = if (item.cityTotal > 0) (diff / item.cityTotal) * 100 else 0.0
                    val isPositive = diff >= 0
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isPositive) {
                            "☀️ Monthly Comparison: +${String.format("%.1f", percent)}% surplus (${String.format("%.1f", diff)} kWh excess clean energy generated)."
                        } else {
                            "☁️ Monthly Comparison: ${String.format("%.1f", percent)}% deficit (${String.format("%.1f", -diff)} kWh below city average)."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPositive) PlantEmerald else CityCoral
                    )
                }
            } else {
                Text(
                    text = "Tap on any month's bar to see comparison statistics",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                )
            }
        }
    }
}

@Composable
fun DailyComparisonChart(
    records: List<SolarRecord>,
    selectedIndex: Int?,
    onIndexSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) {
        EmptyChartPlaceholder(message = "No daily records found. Import CSV for this month or manually log inputs below!")
        return
    }

    val maxVal = records.flatMap { listOf(it.myEnergy, it.cityAverageEnergy ?: 0.0) }.maxOrNull()?.coerceAtLeast(5.0) ?: 30.0
    val chartMax = maxVal * 1.15

    val primaryColor = PlantEmerald
    val secondaryColor = CityCoral
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier.fillMaxWidth()) {
        Column {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .pointerInput(records) {
                        detectTapGestures { offset ->
                            val totalWidth = size.width
                            val step = totalWidth / records.size
                            val index = (offset.x / step).toInt().coerceIn(0, records.size - 1)
                            onIndexSelected(index)
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val paddingBottom = 40f
                val paddingTop = 20f
                val paddingLeft = 10f
                val paddingRight = 10f
                
                val usableHeight = height - paddingTop - paddingBottom
                val itemWidth = (width - paddingLeft - paddingRight) / records.size

                // 4 Grid Lines
                val gridLines = 4
                for (g in 0..gridLines) {
                    val y = paddingTop + (usableHeight / gridLines) * g
                    val value = chartMax - (chartMax / gridLines) * g
                    
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = labelColor.hashCode()
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                        drawText(
                            "${Math.round(value)} kWh",
                            paddingLeft + 15f,
                            y - 8f,
                            paint
                        )
                    }
                }

                // Render lines or filled bars
                // For daily, overlapping outlines or line curves are super precise. Let's draw clean thin lines or close bars.
                records.forEachIndexed { idx, record ->
                    val xCenter = paddingLeft + idx * itemWidth + itemWidth / 2f
                    val barWidth = (itemWidth * 0.45f).coerceAtLeast(1.5f)

                    // User record (Emerald Green)
                    val myHeight = (record.myEnergy / chartMax) * usableHeight
                    val myY = height - paddingBottom - myHeight
                    
                    // City Average (Orange dotted outline/overlay)
                    val cityAvg = record.cityAverageEnergy ?: 0.0
                    val cityHeight = (cityAvg / chartMax) * usableHeight
                    val cityY = height - paddingBottom - cityHeight

                    // Draw line matching city average on background
                    drawRect(
                        color = secondaryColor.copy(alpha = 0.4f),
                        topLeft = Offset(xCenter - barWidth/2f, cityY.toFloat()),
                        size = Size(barWidth, cityHeight.toFloat())
                    )

                    // Draw solid bars for user on top
                    drawRect(
                        color = primaryColor,
                        topLeft = Offset(xCenter - barWidth/2f, myY.toFloat()),
                        size = Size(barWidth, myHeight.toFloat())
                    )

                    // Underline labels (day index, draw e.g. "1", "5", "10", "15", "20", "25", "30")
                    val dayPart = record.date.takeLast(2)
                    val showLabel = dayPart.toIntOrNull()?.let { it == 1 || it % 5 == 0 } ?: false
                    
                    if (showLabel) {
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = labelColor.hashCode()
                                textSize = 26f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            drawText(
                                dayPart,
                                xCenter,
                                height - 10f,
                                paint
                            )
                        }
                    }

                    // Highlight line
                    if (idx == selectedIndex) {
                        drawRect(
                            color = primaryColor.copy(alpha = 0.25f),
                            topLeft = Offset(xCenter - itemWidth / 2f, paddingTop),
                            size = Size(itemWidth, usableHeight),
                            style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f))
                        )
                    }
                }
            }

            // Beautiful interactive detail overlay
            if (selectedIndex != null && selectedIndex < records.size) {
                val item = records[selectedIndex]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Log on: ${item.date}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Your Generated Solar:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${item.myEnergy} kWh",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = PlantEmerald
                            )
                        }
                        Column {
                            Text(
                                text = "${item.cityName} City Avg:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = item.cityAverageEnergy?.let { "$it kWh" } ?: "Not Provided",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = CityCoral
                            )
                        }
                    }

                    item.cityAverageEnergy?.let { cityVal ->
                        val diff = item.myEnergy - cityVal
                        val percent = if (cityVal > 0) (diff / cityVal) * 100 else 0.0
                        val isPositive = diff >= 0
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isPositive) {
                                "☀️ Generation: +${String.format("%.1f", percent)}% surplus (${String.format("%.1f", diff)} kWh ahead of fellow residents)."
                            } else {
                                "☁️ Generation: ${String.format("%.1f", percent)}% deficit (${String.format("%.1f", -diff)} kWh below Mumbai city target)."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPositive) PlantEmerald else CityCoral
                        )
                    }
                }
            } else {
                Text(
                    text = "Tap on any day in the chart to check precise generation metrics",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyChartPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "📊",
                fontSize = 40.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
