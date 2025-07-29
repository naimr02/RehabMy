package com.naimrlet.rehabmy_test.therapist

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.naimrlet.rehabmy_test.therapist.service.TherapistPatientService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePatientsScreen(
    patients: List<PatientInfo>,
    patientService: TherapistPatientService,
    isLoading: Boolean,
    onRefreshPatients: () -> Unit
) {
    var showAddPatientDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var patientToDelete by remember { mutableStateOf<PatientInfo?>(null) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Manage Patients",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { showAddPatientDialog = true },
                modifier = Modifier.wrapContentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Patient",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Patient")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (patients.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No patients assigned yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(patients) { patient ->
                    PatientManagementCard(
                        patient = patient,
                        onDelete = {
                            patientToDelete = patient
                            showDeleteConfirmDialog = true
                        }
                    )
                }
            }
        }
    }

    // Add Patient Dialog
    if (showAddPatientDialog) {
        AddPatientDialog(
            onDismiss = { showAddPatientDialog = false },
            onPatientAdded = { patientId ->
                scope.launch {
                    try {
                        val result = patientService.assignPatientToTherapist(patientId)
                        result.fold(
                            onSuccess = {
                                showAddPatientDialog = false
                                onRefreshPatients()
                            },
                            onFailure = { error ->
                                Log.e("ManagePatientsScreen", "Error adding patient: ${error.message}")
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("ManagePatientsScreen", "Exception adding patient: ${e.message}")
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && patientToDelete != null) {
        DeletePatientConfirmDialog(
            patient = patientToDelete!!,
            onConfirm = {
                scope.launch {
                    try {
                        val result = patientService.unassignPatientFromTherapist(patientToDelete!!.id)
                        result.fold(
                            onSuccess = {
                                showDeleteConfirmDialog = false
                                patientToDelete = null
                                onRefreshPatients()
                            },
                            onFailure = { error ->
                                Log.e("ManagePatientsScreen", "Error removing patient: ${error.message}")
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("ManagePatientsScreen", "Exception removing patient: ${e.message}")
                    }
                }
            },
            onDismiss = {
                showDeleteConfirmDialog = false
                patientToDelete = null
            }
        )
    }
}

@Composable
fun PatientManagementCard(
    patient: PatientInfo,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = patient.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Age: ${patient.age}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Condition: ${patient.condition}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "UID: ${patient.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Patient"
                )
            }
        }
    }
}

@Composable
fun AddPatientDialog(
    onDismiss: () -> Unit,
    onPatientAdded: (String) -> Unit
) {
    var patientUid by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Patient",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = patientUid,
                    onValueChange = {
                        patientUid = it
                        isError = false
                    },
                    label = { Text("Patient Firebase UID") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Please enter a valid UID") }
                    } else null
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (patientUid.trim().isNotEmpty()) {
                                onPatientAdded(patientUid.trim())
                            } else {
                                isError = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
fun DeletePatientConfirmDialog(
    patient: PatientInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Remove Patient")
        },
        text = {
            Text("Are you sure you want to remove ${patient.name} from your patient list?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
