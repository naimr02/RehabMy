package com.naimrlet.rehabmy_test.therapist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.*

@Composable
fun PatientCard(
    patient: PatientInfo,
    onClick: () -> Unit
) {
    // Filter exercises for today only and take first 3 latest ones
    val today = Calendar.getInstance()
    val todayExercises = patient.assignedExercises.filter { exercise ->
        exercise.dueDate?.let { dueDate ->
            val exerciseCalendar = Calendar.getInstance().apply { time = dueDate }
            isSameDay(today, exerciseCalendar)
        } ?: false
    }.sortedByDescending { it.dueDate }.take(3) // Take first 3 latest exercises

    // Calculate completion stats for today's exercises
    val todayCompletedCount = todayExercises.count { it.completed }
    val todayTotalCount = todayExercises.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patient.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${patient.age} years, ${patient.condition}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "View details"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Today's Exercise Summary",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Show today's exercise completion stats
            Text(
                text = if (todayTotalCount > 0) {
                    "$todayCompletedCount of $todayTotalCount exercises completed today"
                } else {
                    "No exercises assigned for today"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Today's Exercises${if (todayExercises.size == 3 && patient.assignedExercises.size > 3) " (showing 3 latest)" else ""}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (todayExercises.isEmpty()) {
                Text(
                    text = "No exercises assigned for today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                todayExercises.forEach { exercise ->
                    ExerciseItem(exercise = exercise)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Show indicator if there are more exercises beyond the 3 shown
                if (patient.assignedExercises.size > todayExercises.size) {
                    val remainingCount = patient.assignedExercises.size - todayExercises.size
                    Text(
                        text = "Tap to view $remainingCount more exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Helper function to check if two calendar dates represent the same day
 */
private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
