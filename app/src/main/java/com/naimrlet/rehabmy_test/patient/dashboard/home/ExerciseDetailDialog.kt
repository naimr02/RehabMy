package com.naimrlet.rehabmy_test.patient.dashboard.home

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naimrlet.rehabmy_test.model.Exercise
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "ExerciseDetailDialog"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
    viewModel: ExerciseViewModel
) {
    val context = LocalContext.current
    val submitting by viewModel.isUploading.collectAsState()
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    
    // New state variables for pain level and comments
    var painLevel by remember { mutableStateOf(1) }
    var comments by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 60.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = exercise.description,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Card {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Assigned",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = exercise.assignedDate?.toDate()?.let { dateFormatter.format(it) } ?: "N/A",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        Card {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Due",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = exercise.dueDate?.toDate()?.let { dateFormatter.format(it) } ?: "N/A",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Repetitions",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = exercise.repetitions.toString(),
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (exercise.completed)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (exercise.completed) "Completed" else "Not Completed",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (exercise.completed)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (!exercise.completed) {
                    // Pain Level Rating
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Pain Level (1-5)",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (i in 1..5) {
                                    OutlinedButton(
                                        onClick = { painLevel = i },
                                        modifier = Modifier.padding(4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (painLevel == i) 
                                                MaterialTheme.colorScheme.primaryContainer 
                                            else 
                                                MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Text(i.toString())
                                    }
                                }
                            }
                            
                            Text(
                                text = when (painLevel) {
                                    1 -> "No pain"
                                    2 -> "Mild pain"
                                    3 -> "Moderate pain"
                                    4 -> "Severe pain"
                                    5 -> "Extreme pain"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    
                    // Comments Field
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
                                text = "Comments",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            OutlinedTextField(
                                value = comments,
                                onValueChange = { comments = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Add any notes about your experience") },
                                minLines = 3
                            )
                        }
                    }
                    
                    // Submit Button
                    if (submitting) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Submitting...",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.completeExerciseWithFeedback(
                                    exerciseId = exercise.id,
                                    painLevel = painLevel,
                                    comments = comments
                                ) { success ->
                                    if (success) {
                                        onDismiss()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            Text("Complete Exercise")
                        }
                    }
                }
            }
        }
    }
}
