package com.naimrlet.rehabmy_test.therapist

// Add Gemini API imports
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.naimrlet.rehabmy_test.therapist.chat.rememberTherapistChatNavigationActions
import com.naimrlet.rehabmy_test.therapist.service.TherapistPatientService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Data models for UI
data class PatientInfo(
    val id: String,
    val name: String,
    val age: Int,
    val condition: String,
    val completedExercises: Int = 0, // Instead of progress float
    val totalExercises: Int = 0,     // Add total exercises
    val assignedExercises: List<ExerciseInfo> = emptyList()
)

// Gemini API client setup
lateinit var geminiModel: GenerativeModel
const val TAG = "TherapistScreen" // Define a TAG for logging

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
suspend fun generateExerciseFeedback(patient: PatientInfo): String {
    Log.d(TAG, "Attempting to generate AI feedback for patient: ${patient.id} - ${patient.name}")
    try {
        if (!::geminiModel.isInitialized) {
            Log.w(TAG, "Gemini API not initialized. Cannot generate feedback.")
            return "AI feedback unavailable. Gemini API not initialized."
        }

        // Create a more concise prompt
        val prompt = buildString {
            append("As a PT assistant, briefly analyze: Patient ${patient.name}, ${patient.age}y, ${patient.condition}, Completed: ${patient.completedExercises}/${patient.totalExercises} exercises\n\n")

            if (patient.assignedExercises.isNotEmpty()) {
                append("Exercises:\n")
                patient.assignedExercises.forEach { exercise ->
                    append("- ${exercise.name}: ${exercise.duration}min, ${exercise.frequency}, ${if (exercise.completed) "completed" else "not completed"}\n")
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
    navController: NavHostController,
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

    // Use the new simplified patient service
    val patientService = remember {
        TherapistPatientService(
            FirebaseFirestore.getInstance(),
            FirebaseAuth.getInstance()
        )
    }

    // State for patients
    val patients = remember { mutableStateListOf<PatientInfo>() }
    var isLoading by remember { mutableStateOf(true) }
    var selectedPatient by remember { mutableStateOf<PatientInfo?>(null) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Navigation state
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Patients", "Chat")

    // Create navigation actions for therapist chat
    val chatNavigationActions = rememberTherapistChatNavigationActions(navController)

    // Fetch data using the new simplified service
    LaunchedEffect(key1 = Unit) {
        try {
            isLoading = true
            errorMessage = null

            Log.d(TAG, "Loading patients using simplified service...")

            val result = patientService.getAssignedPatients()
            result.fold(
                onSuccess = { patientList ->
                    Log.d(TAG, "Successfully loaded ${patientList.size} patients")
                    patients.clear()
                    patients.addAll(patientList)
                    isLoading = false
                },
                onFailure = { error ->
                    Log.e(TAG, "Error loading patients: ${error.message}", error)
                    errorMessage = "Error loading data: ${error.message}"
                    isLoading = false
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception in LaunchedEffect: ${e.message}", e)
            errorMessage = "Error: ${e.message}"
            isLoading = false
        }
    }

    // Check if we should show patient details screen
    selectedPatient?.let { patient ->
        PatientDetailsScreen(
            patient = patient,
            onNavigateBack = { selectedPatient = null },
            geminiApiKey = geminiApiKey
        )
        return // Return early to show only the patient details screen
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
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = if (index == 0) Icons.Default.Person else Icons.AutoMirrored.Filled.Chat,
                                contentDescription = title
                            )
                        },
                        label = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { 
                            if (index == 1) {
                                // Fix: Use the navigation actions instead of direct navigation
                                chatNavigationActions.navigateToTherapistChat()
                            } else {
                                selectedTab = index
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAssignDialog = true },
                    icon = { Icon(Icons.Default.Add, "Assign Exercise") },
                    text = { Text("Assign Exercise") }
                )
            }
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

    // Dialog to assign exercises
    if (showAssignDialog) {
        AssignExerciseDialog(
            patients = patients,
            onDismiss = { showAssignDialog = false }
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
        "completed" to false,
        "assignedBy" to currentUserId,
        "assignedDate" to com.google.firebase.Timestamp.now()
    )

    // Add to Firestore - path: [default]/users/(patientId)/exercises/
    db.collection("users").document(patientId)
        .collection("exercises")
        .add(exerciseData)
        .addOnSuccessListener { documentReference ->
            // Also update the assignedPatient array to include the therapist
            // Path: [default]/users/(patientId)
            db.collection("users").document(patientId)
                .update("assignedPatient", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener {
                    onSuccess(documentReference.id)
                }
                .addOnFailureListener { e ->
                    // Still consider it a success if exercise was added
                    onSuccess(documentReference.id)
                    Log.w(TAG, "Warning: Could not update assignedPatient array: ${e.message}")
                }
        }
        .addOnFailureListener { e ->
            onError(e.message ?: "Unknown error")
        }
}
