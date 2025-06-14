package com.naimrlet.rehabmy_test.therapist

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// Add Gemini API imports
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig

// Data models for UI
data class PatientInfo(
    val id: String,
    val name: String,
    val age: Int,
    val condition: String,
    val progress: Float, // 0.0 to 1.0
    val assignedExercises: List<ExerciseInfo> = emptyList()
)

data class ExerciseInfo(
    val id: String,
    val name: String,
    val description: String,
    val duration: Int, // in minutes
    val frequency: String,
    val completion: Float = 0f // 0.0 to 1.0
)

// Gemini API client setup
private lateinit var geminiModel: GenerativeModel
private const val TAG = "TherapistScreen" // Define a TAG for logging

// Initialize Gemini API with your API key
private fun initializeGeminiApi(apiKey: String) {
    if (apiKey.isEmpty()) {
        Log.e(TAG, "Gemini API key is empty. Initialization skipped.")
        return
    }
    Log.d(TAG, "Initializing Gemini API.")
    val config = generationConfig {
        temperature = 0.7f
        topK = 40
        topP = 0.95f
        maxOutputTokens = 1024
    }
    
    geminiModel = GenerativeModel(
        modelName = "gemini-1.5-pro",
        apiKey = apiKey,
        generationConfig = config
    )
    Log.i(TAG, "Gemini API initialized successfully.")
}

// Function to generate AI feedback for patient exercises
private suspend fun generateExerciseFeedback(patient: PatientInfo): String {
    Log.d(TAG, "Attempting to generate AI feedback for patient: ${patient.id} - ${patient.name}")
    try {
        if (!::geminiModel.isInitialized) {
            Log.w(TAG, "Gemini API not initialized. Cannot generate feedback.")
            return "AI feedback unavailable. Gemini API not initialized."
        }
        
        // Create a more concise prompt
        val prompt = buildString {
            append("As a PT assistant, briefly analyze: Patient ${patient.name}, ${patient.age}y, ${patient.condition}, Progress: ${(patient.progress * 100).toInt()}%\n\n")
            
            if (patient.assignedExercises.isNotEmpty()) {
                append("Exercises:\n")
                patient.assignedExercises.forEach { exercise ->
                    append("- ${exercise.name}: ${exercise.duration}min, ${exercise.frequency}, ${(exercise.completion * 100).toInt()}% complete\n")
                }
            } else {
                append("No exercises assigned yet.\n")
            }
            
            append("\nBriefly provide: 1) Progress assessment 2) Exercise adjustments 3) New exercise suggestions 4) Warning signs")
        }
        Log.v(TAG, "Generated prompt for Gemini API: $prompt")
        
        // Get response from Gemini
        val response = geminiModel.generateContent(prompt)
        val feedbackText = response.text
        
        if (feedbackText == null) {
            Log.w(TAG, "Gemini API returned null text for patient: ${patient.id}")
            return "Unable to generate feedback at this time (null response)."
        }
        
        Log.i(TAG, "Successfully generated AI feedback for patient: ${patient.id}")
        Log.v(TAG, "AI Feedback: $feedbackText")
        return feedbackText
    } catch (e: Exception) {
        Log.e(TAG, "Error generating AI feedback for patient ${patient.id}: ${e.message}", e)
        return "Error generating AI feedback: ${e.message}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TherapistScreen(
    navController: NavController,
    onLogout: () -> Unit,
    geminiApiKey: String = "" // Add parameter for API key
) {
    // Initialize Gemini API
    LaunchedEffect(key1 = geminiApiKey) {
        Log.d(TAG, "TherapistScreen: Raw API key value: '${geminiApiKey}'") // Debug the actual value
        if (geminiApiKey.isNotEmpty()) {
            Log.d(TAG, "TherapistScreen: Gemini API key provided with length: ${geminiApiKey.length}")
            initializeGeminiApi(geminiApiKey)
        } else {
            Log.w(TAG, "TherapistScreen: Gemini API key is NOT provided.")
        }
    }
    
    // State for patients
    val patients = remember { mutableStateListOf<PatientInfo>() }
    var isLoading by remember { mutableStateOf(true) }
    var selectedPatient by remember { mutableStateOf<PatientInfo?>(null) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Fetch data from Firebase Firestore
    LaunchedEffect(key1 = Unit) {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                errorMessage = "User not authenticated"
                isLoading = false
                return@LaunchedEffect
            }
            
            val currentUserId = currentUser.uid
            val db = FirebaseFirestore.getInstance()
            
            // Fetch therapist document to get assigned patients
            val therapistDoc = withContext(Dispatchers.IO) {
                db.collection("users").document(currentUserId).get().await()
            }
            
            // Get assigned patient IDs from the assignedPatient field (array)
            val assignedPatientIds = therapistDoc.get("assignedPatient") as? List<String> ?: emptyList()
            
            if (assignedPatientIds.isEmpty()) {
                isLoading = false
                return@LaunchedEffect
            }
            
            // Fetch data for each assigned patient
            assignedPatientIds.forEach { patientId ->
                try {
                    // Get patient profile data
                    val patientDoc = withContext(Dispatchers.IO) {
                        db.collection("users").document(patientId).get().await()
                    }
                    
                    if (!patientDoc.exists()) return@forEach
                    
                    // Get patient exercises
                    val exercisesSnapshot = withContext(Dispatchers.IO) {
                        db.collection("users")
                            .document(patientId)
                            .collection("exercises")
                            .get().await()
                    }
                    
                    // Map exercises to ExerciseInfo objects
                    val exercises = exercisesSnapshot.documents.mapNotNull { doc ->
                        try {
                            ExerciseInfo(
                                id = doc.id,
                                name = doc.getString("name") ?: return@mapNotNull null,
                                description = doc.getString("description") ?: "",
                                duration = doc.getLong("duration")?.toInt() ?: 0,
                                frequency = doc.getString("frequency") ?: "Daily",
                                completion = doc.getDouble("completion")?.toFloat() ?: 0f
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    // Calculate overall progress based on exercise completion
                    val averageProgress = if (exercises.isNotEmpty()) {
                        exercises.map { it.completion }.average().toFloat()
                    } else {
                        0f
                    }
                    
                    // Create PatientInfo object and add to list
                    val patient = PatientInfo(
                        id = patientId,
                        name = patientDoc.getString("name") ?: "Unknown",
                        age = patientDoc.getLong("age")?.toInt() ?: 0,
                        condition = patientDoc.getString("condition") ?: "Not specified",
                        progress = averageProgress,
                        assignedExercises = exercises
                    )
                    
                    patients.add(patient)
                } catch (e: Exception) {
                    println("Error fetching patient $patientId: ${e.message}")
                }
            }
            
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Error loading data: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Therapist Portal") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAssignDialog = true },
                icon = { Icon(Icons.Default.Add, "Assign Exercise") },
                text = { Text("Assign Exercise") }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Your Patients",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (patients.isEmpty()) {
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
                            modifier = Modifier.weight(1f)
                        ) {
                            items(patients) { patient ->
                                PatientCard(
                                    patient = patient,
                                    onClick = { selectedPatient = patient }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Dialog to show patient details with AI feedback
    if (selectedPatient != null) {
        Log.d(TAG, "Opening PatientDetailsDialog with API key length: ${geminiApiKey.length}")
        PatientDetailsDialog(
            patient = selectedPatient!!,
            onDismiss = { selectedPatient = null },
            geminiApiKey = geminiApiKey // Ensure this is passed correctly
        )
    }
    
    // Dialog to assign exercises
    if (showAssignDialog) {
        AssignExerciseDialog(
            patients = patients,
            onDismiss = { showAssignDialog = false },
            onAssign = { patientId, exerciseName, description, duration, frequency ->
                // Add exercise to Firestore
                addExerciseToPatient(
                    patientId = patientId,
                    exerciseName = exerciseName,
                    description = description,
                    duration = duration,
                    frequency = frequency,
                    onSuccess = { exerciseId ->
                        // Update local state
                        val patientIndex = patients.indexOfFirst { it.id == patientId }
                        if (patientIndex >= 0) {
                            val patient = patients[patientIndex]
                            val newExercise = ExerciseInfo(
                                id = exerciseId,
                                name = exerciseName,
                                description = description,
                                duration = duration,
                                frequency = frequency,
                                completion = 0f
                            )
                            
                            val updatedExercises = patient.assignedExercises + newExercise
                            patients[patientIndex] = patient.copy(assignedExercises = updatedExercises)
                        }
                    },
                    onError = { error ->
                        errorMessage = "Failed to assign exercise: $error"
                    }
                )
                showAssignDialog = false
            }
        )
    }
}

// Function to add exercise to patient in Firestore
private fun addExerciseToPatient(
    patientId: String,
    exerciseName: String,
    description: String,
    duration: Int,
    frequency: String,
    onSuccess: (exerciseId: String) -> Unit,
    onError: (error: String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    if (currentUserId == null) {
        onError("User not authenticated")
        return
    }
    
    // Create exercise data
    val exerciseData = hashMapOf(
        "name" to exerciseName,
        "description" to description,
        "duration" to duration,
        "frequency" to frequency,
        "completion" to 0.0,
        "assignedBy" to currentUserId,
        "assignedDate" to com.google.firebase.Timestamp.now()
    )
    
    // Add to Firestore
    db.collection("users").document(patientId)
        .collection("exercises")
        .add(exerciseData)
        .addOnSuccessListener { documentReference ->
            onSuccess(documentReference.id)
        }
        .addOnFailureListener { e ->
            onError(e.message ?: "Unknown error")
        }
}

@Composable
fun PatientCard(
    patient: PatientInfo,
    onClick: () -> Unit
) {
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
                        text = "${patient.age} years • ${patient.condition}",
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
                text = "Overall Progress",
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = patient.progress,
                modifier = Modifier.fillMaxWidth(),
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Assigned Exercises",
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            if (patient.assignedExercises.isEmpty()) {
                Text(
                    text = "No exercises assigned yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                patient.assignedExercises.forEach { exercise ->
                    ExerciseItem(exercise = exercise)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun PatientDetailsDialog(
    patient: PatientInfo,
    onDismiss: () -> Unit,
    geminiApiKey: String = "" // This parameter is already here
) {
    var aiFeedback by remember { mutableStateOf("Generating AI feedback...") }
    var isLoadingFeedback by remember { mutableStateOf(true) }
    
    // Add debug log to verify the API key at dialog level
    Log.d(TAG, "PatientDetailsDialog: API key received, length: ${geminiApiKey.length}, isEmpty: ${geminiApiKey.isEmpty()}")
    
    // Generate AI feedback when dialog opens
    LaunchedEffect(key1 = patient.id) {
        if (geminiApiKey.isNotEmpty()) {
            Log.d(TAG, "PatientDetailsDialog: Attempting to generate feedback for ${patient.name}")
            isLoadingFeedback = true
            aiFeedback = generateExerciseFeedback(patient)
            isLoadingFeedback = false
        } else {
            Log.w(TAG, "PatientDetailsDialog: Gemini API key is empty. Cannot generate feedback for ${patient.name}")
            aiFeedback = "AI feedback unavailable. Please configure Gemini API key."
            isLoadingFeedback = false
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(patient.name) },
        text = {
            LazyColumn {
                item {
                    Text(
                        text = "${patient.age} years • ${patient.condition}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // AI Feedback Section
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
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ExerciseItem(exercise: ExerciseInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = exercise.description,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${exercise.duration} min • ${exercise.frequency}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "Completion:",
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.width(4.dp))

            LinearProgressIndicator(
                progress = exercise.completion,
                modifier = Modifier.width(80.dp),
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "${(exercise.completion * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun AssignExerciseDialog(
    patients: List<PatientInfo>,
    onDismiss: () -> Unit,
    onAssign: (patientId: String, name: String, description: String, duration: Int, frequency: String) -> Unit
) {
    var selectedPatientId by remember { mutableStateOf("") }
    var exerciseName by remember { mutableStateOf("") }
    var exerciseDescription by remember { mutableStateOf("") }
    var exerciseDuration by remember { mutableStateOf("10") }
    var exerciseFrequency by remember { mutableStateOf("Daily") }
    var expanded by remember { mutableStateOf(false) }
    
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validate inputs
                    if (selectedPatientId.isNotEmpty() && exerciseName.isNotEmpty()) {
                        val duration = exerciseDuration.toIntOrNull() ?: 10
                        onAssign(
                            selectedPatientId,
                            exerciseName,
                            exerciseDescription,
                            duration,
                            exerciseFrequency
                        )
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

