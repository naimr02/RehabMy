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
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignExerciseDialog(
    patients: List<PatientInfo>,
    assignedDate: Date = Date(), // Default to current date if not specified
    onDismiss: () -> Unit,
    onAssignSuccess: (exerciseId: String, exerciseName: String, exerciseData: Map<String, Any>) -> Unit = { _, _, _ -> }, // Updated callback
    preSelectedPatientId: String = "" // New parameter to pre-select a patient
) {
    var selectedPatientId by remember { mutableStateOf(preSelectedPatientId) }
    var exerciseName by remember { mutableStateOf("") }
    var exerciseDescription by remember { mutableStateOf("") }
    var exerciseDuration by remember { mutableStateOf("10") }
    var exerciseFrequency by remember { mutableStateOf("Daily") }
    var expanded by remember { mutableStateOf(false) }
    
    // Date range picker states
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var selectedStartDate by remember { mutableStateOf(assignedDate) }
    var selectedEndDate by remember { mutableStateOf(assignedDate) }
    val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    
    // Date picker states for both start and end dates
    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedStartDate.time
    )

    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedEndDate.time
    )
    
    // Start date picker dialog
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let { millis ->
                        val newDate = Date(millis)
                        selectedStartDate = newDate
                        // If end date is before start date, update end date to match start date
                        if (selectedEndDate.before(newDate)) {
                            selectedEndDate = newDate
                        }
                    }
                    showStartDatePicker = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    // End date picker dialog
    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let { millis ->
                        val newDate = Date(millis)
                        // Only update if end date is not before start date
                        if (!newDate.before(selectedStartDate)) {
                            selectedEndDate = newDate
                        }
                    }
                    showEndDatePicker = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    // Get Firebase Firestore instance
    val db = FirebaseFirestore.getInstance()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Exercise") },
        text = {
            Column {
                // Patient selector - only show if not pre-selected
                if (preSelectedPatientId.isEmpty()) {
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
                } else {
                    // If patient is pre-selected, just show the name
                    val patientName = patients.find { it.id == preSelectedPatientId }?.name ?: "Selected Patient"
                    Text(
                        text = "Patient: $patientName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }

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

                Spacer(modifier = Modifier.height(16.dp))

                // Date range selection fields
                Text(
                    text = "Exercise Date Range",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Start date picker
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start Date",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = dateFormat.format(selectedStartDate),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showStartDatePicker = true }
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // End date picker
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "End Date",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = dateFormat.format(selectedEndDate),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEndDatePicker = true }
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calculate and show number of days
                val daysDifference = ((selectedEndDate.time - selectedStartDate.time) / (1000 * 60 * 60 * 24)).toInt() + 1
                Text(
                    text = "Exercise will be assigned for $daysDifference day(s). Each exercise will be due at 11:59 PM of its respective day.",
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
                        
                        // Create exercises for each day in the range
                        val calendar = Calendar.getInstance()
                        calendar.time = selectedStartDate

                        // Reset to start of day
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)

                        val endCalendar = Calendar.getInstance()
                        endCalendar.time = selectedEndDate
                        endCalendar.set(Calendar.HOUR_OF_DAY, 0)
                        endCalendar.set(Calendar.MINUTE, 0)
                        endCalendar.set(Calendar.SECOND, 0)
                        endCalendar.set(Calendar.MILLISECOND, 0)

                        var exercisesCreated = 0
                        var totalExercises = 0

                        // Count total exercises to create
                        val tempCalendar = calendar.clone() as Calendar
                        while (!tempCalendar.time.after(endCalendar.time)) {
                            totalExercises++
                            tempCalendar.add(Calendar.DAY_OF_MONTH, 1)
                        }

                        // Create exercises for each day
                        while (!calendar.time.after(endCalendar.time)) {
                            // Create due date for this specific day (11:59 PM)
                            val dueDateCalendar = calendar.clone() as Calendar
                            dueDateCalendar.set(Calendar.HOUR_OF_DAY, 23)
                            dueDateCalendar.set(Calendar.MINUTE, 59)
                            dueDateCalendar.set(Calendar.SECOND, 59)
                            val dueDate = dueDateCalendar.time

                            // Create exercise data map for this day
                            val exerciseData = hashMapOf(
                                "patientId" to selectedPatientId,
                                "name" to exerciseName,
                                "description" to exerciseDescription,
                                "duration" to duration,
                                "frequency" to exerciseFrequency,
                                "assignedDate" to calendar.time,
                                "dueDate" to dueDate,
                                "completed" to false,
                                "status" to "Assigned",
                                "timestamp" to Date()
                            )

                            // Save to Firestore
                            db.collection("users")
                                .document(selectedPatientId)
                                .collection("exercises")
                                .add(exerciseData)
                                .addOnSuccessListener { docRef ->
                                    exercisesCreated++

                                    // If this is the first exercise created, update patient assignment
                                    if (exercisesCreated == 1) {
                                        updatePatientAssignmentStatus(selectedPatientId)
                                    }

                                    // Call success callback for the first exercise
                                    if (exercisesCreated == 1) {
                                        onAssignSuccess(docRef.id, exerciseName, exerciseData)
                                    }

                                    // Close dialog when all exercises are created
                                    if (exercisesCreated == totalExercises) {
                                        onDismiss()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("AssignExercise", "Error creating exercise for ${calendar.time}: ${e.message}")
                                    // Still count as processed to avoid hanging
                                    exercisesCreated++
                                    if (exercisesCreated == totalExercises) {
                                        onDismiss()
                                    }
                                }

                            // Move to next day
                            calendar.add(Calendar.DAY_OF_MONTH, 1)
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

// Updated function to update the patient's assignment status
private fun updatePatientAssignmentStatus(patientId: String) {
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    
    // Add the therapist's ID to the assignedPatient array for this patient
    // Path: [default]/users/(patientId) - updating the assignedPatient array field
    db.collection("users")
        .document(patientId)
        .update("assignedPatient", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId))
        .addOnFailureListener { e ->
            Log.e("AssignExercise", "Error updating patient assignment status: ${e.message}")
        }
}
