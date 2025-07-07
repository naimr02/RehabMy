package com.naimrlet.rehabmy_test.patient.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.LineType
import com.naimrlet.rehabmy_test.patient.progress.ProgressViewModel
import com.naimrlet.rehabmy_test.patient.progress.ProgressPeriod
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AboutScreen() {
    val progressViewModel: ProgressViewModel = hiltViewModel()
    val scrollState = rememberScrollState()

    val weeklyProgress by progressViewModel.weeklyProgress.collectAsState()
    val monthlyProgress by progressViewModel.monthlyProgress.collectAsState()
    val yearlyProgress by progressViewModel.yearlyProgress.collectAsState()
    val selectedPeriod by progressViewModel.selectedPeriod.collectAsState()
    val loading by progressViewModel.loading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProgressHeader()
        Spacer(modifier = Modifier.height(32.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            PeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { progressViewModel.setSelectedPeriod(it) }
            )
            Spacer(modifier = Modifier.height(24.dp))

            ProgressChart(
                selectedPeriod = selectedPeriod,
                weeklyProgress = weeklyProgress,
                monthlyProgress = monthlyProgress,
                yearlyProgress = yearlyProgress
            )
            Spacer(modifier = Modifier.height(32.dp))

            ProgressSummaryCard(
                selectedPeriod = selectedPeriod,
                weeklyProgress = weeklyProgress,
                monthlyProgress = monthlyProgress,
                yearlyProgress = yearlyProgress
            )
        }
    }
}

@Composable
private fun ProgressHeader() {
    Text(
        text = "Exercise Progress",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Text(
        text = "Track your exercise completion over time",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PeriodSelector(
    selectedPeriod: ProgressPeriod,
    onPeriodSelected: (ProgressPeriod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ProgressPeriod.values().forEach { period ->
            FilterChip(
                onClick = { onPeriodSelected(period) },
                label = {
                    Text(
                        text = when (period) {
                            ProgressPeriod.WEEKLY -> "Weekly"
                            ProgressPeriod.MONTHLY -> "Monthly"
                            ProgressPeriod.YEARLY -> "Yearly"
                        }
                    )
                },
                selected = selectedPeriod == period,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun ProgressChart(
    selectedPeriod: ProgressPeriod,
    weeklyProgress: List<com.naimrlet.rehabmy_test.patient.progress.ProgressData>,
    monthlyProgress: List<com.naimrlet.rehabmy_test.patient.progress.ProgressData>,
    yearlyProgress: List<com.naimrlet.rehabmy_test.patient.progress.ProgressData>
) {
    val progressData = when (selectedPeriod) {
        ProgressPeriod.WEEKLY -> weeklyProgress
        ProgressPeriod.MONTHLY -> monthlyProgress
        ProgressPeriod.YEARLY -> yearlyProgress
    }

    if (progressData.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No exercise data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val points = progressData.mapIndexed { index, data ->
        Point(index.toFloat(), data.completionPercentage)
    }

    val maxValue = 100f
    val stepSize = 20f

    val xAxisData = AxisData.Builder()
        .axisStepSize(1.dp)
        .backgroundColor(Color.Transparent)
        .steps(progressData.size - 1)
        .labelData { i ->
            if (i < progressData.size) {
                val dateFormat = when (selectedPeriod) {
                    ProgressPeriod.WEEKLY -> SimpleDateFormat("MM/dd", Locale.getDefault())
                    ProgressPeriod.MONTHLY -> SimpleDateFormat("MMM", Locale.getDefault())
                    ProgressPeriod.YEARLY -> SimpleDateFormat("MMM yyyy", Locale.getDefault())
                }
                dateFormat.format(progressData[i.toInt()].date)
            } else ""
        }
        .labelAndAxisLinePadding(15.dp)
        .build()

    val yAxisData = AxisData.Builder()
        .steps((maxValue / stepSize).toInt())
        .backgroundColor(Color.Transparent)
        .labelAndAxisLinePadding(20.dp)
        .labelData { i ->
            "${(i * stepSize).toInt()}%"
        }
        .build()

    val lineChartData = LineChartData(
        linePlotData = LinePlotData(
            lines = listOf(
                co.yml.charts.ui.linechart.model.Line(
                    dataPoints = points,
                    lineStyle = LineStyle(
                        color = MaterialTheme.colorScheme.primary,
                        lineType = LineType.SmoothCurve(isDotted = false)
                    ),
                    intersectionPoint = co.yml.charts.ui.linechart.model.IntersectionPoint(
                        color = MaterialTheme.colorScheme.primary
                    ),
                    selectionHighlightPoint = co.yml.charts.ui.linechart.model.SelectionHighlightPoint(
                        color = MaterialTheme.colorScheme.primary
                    ),
                    shadowUnderLine = co.yml.charts.ui.linechart.model.ShadowUnderLine(
                        alpha = 0.5f,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    ),
                    selectionHighlightPopUp = co.yml.charts.ui.linechart.model.SelectionHighlightPopUp()
                )
            )
        ),
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        gridLines = co.yml.charts.ui.linechart.model.GridLines(
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        backgroundColor = Color.Transparent
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Exercise Completion Rate",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                lineChartData = lineChartData
            )
        }
    }
}

@Composable
private fun ProgressSummaryCard(
    selectedPeriod: ProgressPeriod,
    weeklyProgress: List<com.naimrlet.rehabmy_test.patient.progress.ProgressData>,
    monthlyProgress: List<com.naimrlet.rehabmy_test.patient.progress.ProgressData>,
    yearlyProgress: List<com.naimrlet.rehabmy_test.patient.progress.ProgressData>
) {
    val progressData = when (selectedPeriod) {
        ProgressPeriod.WEEKLY -> weeklyProgress
        ProgressPeriod.MONTHLY -> monthlyProgress
        ProgressPeriod.YEARLY -> yearlyProgress
    }

    if (progressData.isEmpty()) return

    val totalExercises = progressData.sumOf { it.totalExercises }
    val completedExercises = progressData.sumOf { it.completedExercises }
    val averageCompletion = if (progressData.isNotEmpty()) {
        progressData.map { it.completionPercentage }.average().toFloat()
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Summary",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    title = "Total Exercises",
                    value = totalExercises.toString(),
                    modifier = Modifier.weight(1f)
                )
                SummaryItem(
                    title = "Completed",
                    value = completedExercises.toString(),
                    modifier = Modifier.weight(1f)
                )
                SummaryItem(
                    title = "Average Rate",
                    value = "${averageCompletion.toInt()}%",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
