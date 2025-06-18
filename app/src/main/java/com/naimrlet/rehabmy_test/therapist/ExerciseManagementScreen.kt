package com.naimrlet.rehabmy_test.therapist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ExerciseManagementScreen(
    patient: PatientInfo,
    onDismiss: () -> Unit
) {
    // Get current date
    val today = remember { Calendar.getInstance() }
    // Track selected date
    var selectedDate by remember { mutableStateOf(today.time) }
    // Track current visible month
    var currentMonth by remember { mutableIntStateOf(today.get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }

    // Group exercises by due date
    val exercisesByDate = remember(patient.assignedExercises, selectedDate) {
        patient.assignedExercises.groupBy { exercise ->
            // Convert each exercise due date to a Calendar for comparison
            val exerciseCalendar = Calendar.getInstance().apply {
                if (exercise.dueDate != null) {
                    time = exercise.dueDate
                }
            }

            // Format as YYYY-MM-DD for map key
            val year = exerciseCalendar.get(Calendar.YEAR)
            val month = exerciseCalendar.get(Calendar.MONTH)
            val day = exerciseCalendar.get(Calendar.DAY_OF_MONTH)
            "$year-${month+1}-$day"
        }
    }

    // Get key for selected date to filter exercises
    val selectedDateKey = remember(selectedDate) {
        val cal = Calendar.getInstance().apply { time = selectedDate }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        "$year-${month+1}-$day"
    }

    // Filter exercises for selected date
    val exercisesForSelectedDate = remember(selectedDateKey, exercisesByDate) {
        exercisesByDate[selectedDateKey] ?: emptyList()
    }

    // Dates with exercises
    val datesWithExercises = remember(exercisesByDate) {
        exercisesByDate.keys.mapNotNull { dateKey ->
            try {
                val parts = dateKey.split("-")
                if (parts.size == 3) {
                    val year = parts[0].toInt()
                    val month = parts[1].toInt() - 1  // Calendar months are 0-based
                    val day = parts[2].toInt()

                    Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                    }.time
                } else null
            } catch (e: Exception) {
                null
            }
        }.toSet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${patient.name}'s Exercises") },
        text = {
            Column {
                // Calendar header with month navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        // Go to previous month
                        if (currentMonth == 0) {
                            currentMonth = 11
                            currentYear--
                        } else {
                            currentMonth--
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month"
                        )
                    }

                    Text(
                        text = Calendar.getInstance().apply {
                            set(Calendar.YEAR, currentYear)
                            set(Calendar.MONTH, currentMonth)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }.time.let { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(it) },
                        style = MaterialTheme.typography.titleMedium
                    )

                    IconButton(onClick = {
                        // Go to next month
                        if (currentMonth == 11) {
                            currentMonth = 0
                            currentYear++
                        } else {
                            currentMonth++
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month"
                        )
                    }
                }

                // Day of week headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Calendar grid
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                }

                val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK) - 1
                val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

                // Create a grid of weeks
                for (i in 0 until 6) { // Up to 6 weeks in a month
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (j in 0 until 7) { // 7 days in a week
                            val day = i * 7 + j - firstDayOfMonth + 1

                            if (day in 1..daysInMonth) {
                                // Create date for this cell
                                val cellDate = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, currentYear)
                                    set(Calendar.MONTH, currentMonth)
                                    set(Calendar.DAY_OF_MONTH, day)
                                }.time

                                // Check if this date has exercises
                                val hasExercises = datesWithExercises.any { date ->
                                    val cal1 = Calendar.getInstance().apply { time = date }
                                    val cal2 = Calendar.getInstance().apply { time = cellDate }

                                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                                            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                                            cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
                                }

                                // Check if this is the selected date
                                val isSelected = Calendar.getInstance().let { cal ->
                                    cal.time = selectedDate
                                    cal.get(Calendar.YEAR) == currentYear &&
                                            cal.get(Calendar.MONTH) == currentMonth &&
                                            cal.get(Calendar.DAY_OF_MONTH) == day
                                }

                                // Check if this is today
                                val isToday = Calendar.getInstance().let { cal ->
                                    cal.get(Calendar.YEAR) == currentYear &&
                                            cal.get(Calendar.MONTH) == currentMonth &&
                                            cal.get(Calendar.DAY_OF_MONTH) == day
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(4.dp)
                                        .border(
                                            width = if (isToday) 1.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                        .background(
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                hasExercises -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                                else -> Color.Transparent
                                            },
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedDate = cellDate
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                            hasExercises -> MaterialTheme.colorScheme.onSecondaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            } else {
                                // Empty cell
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date indicator
                Text(
                    text = "Exercises for ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(selectedDate)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Show exercises for selected date
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    if (exercisesForSelectedDate.isEmpty()) {
                        item {
                            Text(
                                text = "No exercises due on this date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(exercisesForSelectedDate) { exercise ->
                            ExerciseItem(exercise = exercise)
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* Add exercise functionality */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Exercise")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}