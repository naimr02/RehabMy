package com.naimrlet.rehabmy_test.therapist

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PatientDetailsDialog(
    patient: PatientInfo,
    onDismiss: () -> Unit,
    geminiApiKey: String = ""
) {
    var showAiFeedback by remember { mutableStateOf(false) }
    var aiFeedback by remember { mutableStateOf("") }
    var isLoadingFeedback by remember { mutableStateOf(false) }
    var showExerciseInterface by remember { mutableStateOf(false) }

    // Only generate AI feedback when the button is clicked
    LaunchedEffect(key1 = showAiFeedback) {
        if (showAiFeedback && aiFeedback.isEmpty()) {
            if (geminiApiKey.isNotEmpty()) {
                Log.d(TAG, "PatientDetailsDialog: Generating feedback for ${patient.name}")
                isLoadingFeedback = true
                aiFeedback = generateExerciseFeedback(patient)
                isLoadingFeedback = false
            } else {
                Log.w(TAG, "PatientDetailsDialog: Gemini API key is empty. Cannot generate feedback for ${patient.name}")
                aiFeedback = "AI feedback unavailable. Please configure Gemini API key."
                isLoadingFeedback = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(patient.name) },
        text = {
            LazyColumn {
                item {
                    Text(
                        text = "${patient.age} years, ${patient.condition}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Only show AI Feedback section if requested
                    if (showAiFeedback) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "AI Insights & Recommendations",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (isLoadingFeedback) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.width(24.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = aiFeedback,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        text = "Assigned Exercises",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (patient.assignedExercises.isEmpty()) {
                        Text(
                            text = "No exercises assigned yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        patient.assignedExercises.forEach { exercise ->
                            Spacer(modifier = Modifier.height(8.dp))
                            ExerciseItem(exercise = exercise)
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
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
                // AI Feedback button
                OutlinedButton(
                    onClick = { showAiFeedback = !showAiFeedback },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (showAiFeedback) "Hide AI Feedback" else "Get AI Feedback")
                }

                // Exercise button
                Button(
                    onClick = { showExerciseInterface = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Exercises")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    // Show exercise interface when the button is clicked
    if (showExerciseInterface) {
        ExerciseManagementScreen(
            patient = patient,
            onDismiss = { showExerciseInterface = false }
        )
    }
}