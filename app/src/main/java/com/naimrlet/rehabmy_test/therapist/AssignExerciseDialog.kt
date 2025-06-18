package com.naimrlet.rehabmy_test.therapist

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignExerciseDialog(
    patients: List<PatientInfo>,
    assignedDate: Date = Date(), // Default to current date if not specified
    onDismiss: () -> Unit,
    onAssignSuccess: () -> Unit = {} // Optional callback for successful assignment
) {
    var selectedPatientId by remember { mutableStateOf("") }
    var exerciseName by remember { mutableStateOf("") }
    var exerciseDescription by remember { mutableStateOf("") }
    var exerciseDuration by remember { mutableStateOf("10") }
    var exerciseFrequency by remember { mutableStateOf("Daily") }
    var expanded by remember { mutableStateOf(false) }
    
    // Date picker states
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedAssignedDate by remember { mutableStateOf(assignedDate) }
    val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    
    // Calculate due date (assigned date at 11:59 PM)
    val calendar = Calendar.getInstance()
    calendar.time = selectedAssignedDate
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    val dueDate = calendar.time
    
    // Date picker state and dialog
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedAssignedDate.time
    )
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = Date(millis)
                        selectedAssignedDate = newDate
                    }
                    showDatePicker = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Get Firebase Firestore instance
    val db = FirebaseFirestore.getInstance()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Exercise") },
        text = {
            Column {
                // Patient selector
                Text(
                    text = "Select Patient",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Box {
                    Text(
                        text = if (selectedPatientId.isNotEmpty()) {
                            patients.find { it.id == selectedPatientId }?.name ?: "Select patient"
                        } else {
                            "Select patient"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true }
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(16.dp)
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        patients.forEach { patient ->
                            DropdownMenuItem(
                                text = { Text(patient.name) },
                                onClick = {
                                    selectedPatientId = patient.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Exercise fields
                Text(
                    text = "Exercise Name",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                androidx.compose.material3.TextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Knee Extensions") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Description",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                androidx.compose.material3.TextField(
                    value = exerciseDescription,
                    onValueChange = { exerciseDescription = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Describe how to perform the exercise") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Duration (min)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )

                        androidx.compose.material3.TextField(
                            value = exerciseDuration,
                            onValueChange = { exerciseDuration = it },
                            placeholder = { Text("10") }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Frequency",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )

                        androidx.compose.material3.TextField(
                            value = exerciseFrequency,
                            onValueChange = { exerciseFrequency = it },
                            placeholder = { Text("Daily") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                // Date selection field
                Text(
                    text = "Assigned Date",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = dateFormat.format(selectedAssignedDate),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Exercise will be due at 11:59 PM of the assigned date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validate inputs
                    if (selectedPatientId.isNotEmpty() && exerciseName.isNotEmpty()) {
                        val duration = exerciseDuration.toIntOrNull() ?: 10
                        
                        // Create exercise data map
                        val exerciseData = hashMapOf(
                            "patientId" to selectedPatientId,
                            "name" to exerciseName,
                            "description" to exerciseDescription,
                            "duration" to duration,
                            "frequency" to exerciseFrequency,
                            "assignedDate" to selectedAssignedDate,
                            "dueDate" to dueDate,
                            "status" to "Assigned",
                            "timestamp" to Date() // Add timestamp for sorting/tracking
                        )
                        
                        // Save to Firestore in the patient's exercises subcollection
                        db.collection("users")
                            .document(selectedPatientId)
                            .collection("exercises")
                            .add(exerciseData)
                            .addOnSuccessListener {
                                // Call success callback
                                onAssignSuccess()
                                // Dismiss dialog
                                onDismiss()
                            }
                            .addOnFailureListener { e ->
                                // In a real app, you might want to show an error message here
                                // For now, just dismiss the dialog
                                onDismiss()
                            }
                    }
                },
                enabled = selectedPatientId.isNotEmpty() && exerciseName.isNotEmpty()
            ) {
                Text("Assign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
