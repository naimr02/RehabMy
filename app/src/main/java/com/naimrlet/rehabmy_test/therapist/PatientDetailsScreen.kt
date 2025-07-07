package com.naimrlet.rehabmy_test.therapist

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailsScreen(
    patient: PatientInfo,
    onNavigateBack: () -> Unit,
    geminiApiKey: String = ""
) {
    // Initialize the view model with the patient ID
    val viewModel: ExerciseViewModel = viewModel(
        factory = ExerciseViewModel.Factory(patient.id)
    )

    // Get exercises from the view model
    val exercises by viewModel.exercises.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Load exercises when screen is shown
    LaunchedEffect(patient.id) {
        viewModel.loadExercises()
    }

    // Clear any error
    LaunchedEffect(error) {
        if (error != null) {
            viewModel.clearError()
        }
    }

    // Calendar state
    val today = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf(today.time) }
    var currentMonth by remember { mutableIntStateOf(today.get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }

    // AI Feedback state
    var showAiFeedback by remember { mutableStateOf(false) }
    var showFeedbackOptions by remember { mutableStateOf(false) }
    var aiFeedback by remember { mutableStateOf("") }
    var isLoadingFeedback by remember { mutableStateOf(false) }
    var feedbackPeriod by remember { mutableStateOf("week") }

    // Exercise assignment state
    var showAssignExerciseDialog by remember { mutableStateOf(false) }

    // Group exercises by due date
    val exercisesByDate = remember(exercises, selectedDate) {
        exercises.groupBy { exercise ->
            val exerciseCalendar = Calendar.getInstance().apply {
                if (exercise.dueDate != null) {
                    time = exercise.dueDate
                }
            }
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
                    val month = parts[1].toInt() - 1
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

    // AI Feedback generation
    LaunchedEffect(key1 = showAiFeedback, key2 = feedbackPeriod) {
        if (showAiFeedback && aiFeedback.isEmpty()) {
            if (geminiApiKey.isNotEmpty()) {
                Log.d("PatientDetailsScreen", "Generating $feedbackPeriod feedback for ${patient.name}")
                isLoadingFeedback = true

                val updatedPatient = patient.copy(
                    assignedExercises = exercises,
                    totalExercises = exercises.size,
                    completedExercises = exercises.count { it.completed }
                )

                aiFeedback = generatePeriodExerciseFeedback(updatedPatient, feedbackPeriod, geminiApiKey)
                isLoadingFeedback = false
            } else {
                Log.w("PatientDetailsScreen", "Gemini API key is empty")
                aiFeedback = "AI feedback unavailable. Please configure Gemini API key."
                isLoadingFeedback = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = patient.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAssignExerciseDialog = true },
                icon = { Icon(Icons.Default.Add, "Assign Exercise") },
                text = { Text("Assign Exercise") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Patient basic info card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "${patient.age} years old",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Condition: ${patient.condition}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Progress: ${exercises.count { it.completed }}/${exercises.size} exercises completed",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Feedback card (only show if requested)
            if (showAiFeedback) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AI Insights (${feedbackPeriod.capitalize()})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            TextButton(onClick = { showAiFeedback = false }) {
                                Text("Hide")
                            }
                        }

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

            // Calendar section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Calendar header with month navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
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
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = {
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

                    Spacer(modifier = Modifier.height(8.dp))

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
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
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

                    for (i in 0 until 6) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (j in 0 until 7) {
                                val day = i * 7 + j - firstDayOfMonth + 1

                                if (day in 1..daysInMonth) {
                                    val cellDate = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, currentYear)
                                        set(Calendar.MONTH, currentMonth)
                                        set(Calendar.DAY_OF_MONTH, day)
                                    }.time

                                    val hasExercises = datesWithExercises.any { date ->
                                        val cal1 = Calendar.getInstance().apply { time = date }
                                        val cal2 = Calendar.getInstance().apply { time = cellDate }

                                        cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                                                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                                                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
                                    }

                                    val isSelected = Calendar.getInstance().let { cal ->
                                        cal.time = selectedDate
                                        cal.get(Calendar.YEAR) == currentYear &&
                                                cal.get(Calendar.MONTH) == currentMonth &&
                                                cal.get(Calendar.DAY_OF_MONTH) == day
                                    }

                                    val isToday = Calendar.getInstance().let { cal ->
                                        cal.get(Calendar.YEAR) == currentYear &&
                                                cal.get(Calendar.MONTH) == currentMonth &&
                                                cal.get(Calendar.DAY_OF_MONTH) == day
                                    }

                                    // Check completion status for this date
                                    val dateKey = "$currentYear-${currentMonth+1}-$day"
                                    val dayExercises = exercisesByDate[dateKey] ?: emptyList()
                                    val hasCompletedExercises = dayExercises.any { it.completed }
                                    val hasIncompleteExercises = dayExercises.any { !it.completed }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .border(
                                                width = if (isToday) 2.dp else 0.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                            .background(
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                    hasCompletedExercises && !hasIncompleteExercises -> MaterialTheme.colorScheme.secondaryContainer
                                                    hasIncompleteExercises -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                                    hasExercises -> MaterialTheme.colorScheme.surfaceVariant
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
                                            style = MaterialTheme.typography.bodySmall,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                                hasCompletedExercises && !hasIncompleteExercises -> MaterialTheme.colorScheme.onSecondaryContainer
                                                hasIncompleteExercises -> MaterialTheme.colorScheme.onErrorContainer
                                                hasExercises -> MaterialTheme.colorScheme.onSurfaceVariant
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Feedback button with dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showFeedbackOptions = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Get AI Feedback")
                    }

                    DropdownMenu(
                        expanded = showFeedbackOptions,
                        onDismissRequest = { showFeedbackOptions = false }
                    ) {
                        listOf("week", "month", "year").forEach { period ->
                            DropdownMenuItem(
                                text = { Text("${period.capitalize()} Analysis") },
                                onClick = {
                                    feedbackPeriod = period
                                    aiFeedback = "" // Reset feedback to trigger regeneration
                                    showAiFeedback = true
                                    showFeedbackOptions = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected date exercises
            Text(
                text = "Exercises for ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(selectedDate)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (exercisesForSelectedDate.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No exercises scheduled for this date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(exercisesForSelectedDate) { exercise ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                ExerciseItem(exercise = exercise)
                            }
                        }
                    }
                }
            }
        }
    }

    // Show assign exercise dialog
    if (showAssignExerciseDialog) {
        AssignExerciseDialog(
            patients = listOf(patient),
            assignedDate = selectedDate,
            onDismiss = { showAssignExerciseDialog = false },
            onAssignSuccess = { _, _, _ ->
                viewModel.loadExercises()
            },
            preSelectedPatientId = patient.id
        )
    }
}

// Function to generate AI feedback for different periods
suspend fun generatePeriodExerciseFeedback(patient: PatientInfo, period: String, geminiApiKey: String): String {
    Log.d("PatientDetailsScreen", "Generating $period feedback for patient: ${patient.id} - ${patient.name}")
    try {
        if (geminiApiKey.isEmpty()) {
            Log.w("PatientDetailsScreen", "Gemini API key is empty")
            return "AI feedback unavailable. Please configure Gemini API key."
        }

        // Initialize Gemini model locally
        val geminiModel = try {
            com.google.ai.client.generativeai.GenerativeModel(
                modelName = "gemini-1.5-pro",
                apiKey = geminiApiKey,
                generationConfig = com.google.ai.client.generativeai.type.generationConfig {
                    temperature = 0.7f
                    topK = 40
                    topP = 0.95f
                    maxOutputTokens = 1024
                }
            )
        } catch (e: Exception) {
            Log.w("PatientDetailsScreen", "Failed to initialize Gemini API: ${e.message}")
            return "AI feedback unavailable. Gemini API initialization failed: ${e.message}"
        }

        // Filter exercises based on period
        val now = Calendar.getInstance()
        val startDate = Calendar.getInstance().apply {
            when (period) {
                "week" -> {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    add(Calendar.WEEK_OF_YEAR, 0)
                }
                "month" -> {
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, 0)
                }
                "year" -> {
                    set(Calendar.DAY_OF_YEAR, 1)
                    add(Calendar.YEAR, 0)
                }
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val filteredExercises = patient.assignedExercises.filter { exercise ->
            exercise.dueDate?.let { dueDate ->
                dueDate.after(startDate.time) && dueDate.before(now.time)
            } ?: false
        }

        val prompt = buildString {
            append("As a physiotherapy assistant, provide a comprehensive $period analysis for:\n")
            append("Patient: ${patient.name}, ${patient.age}y, Condition: ${patient.condition}\n\n")

            append("$period Summary (${SimpleDateFormat("MMM d", Locale.getDefault()).format(startDate.time)} - ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(now.time)}):\n")
            append("- Total exercises assigned: ${filteredExercises.size}\n")
            append("- Completed: ${filteredExercises.count { it.completed }}\n")
            append("- Completion rate: ${if (filteredExercises.isNotEmpty()) String.format("%.1f", (filteredExercises.count { it.completed }.toFloat() / filteredExercises.size) * 100) else "0"}%\n\n")

            if (filteredExercises.isNotEmpty()) {
                append("Exercises in this $period:\n")
                filteredExercises.forEach { exercise ->
                    append("- ${exercise.name}: ${exercise.duration}min, ${exercise.frequency}")
                    if (exercise.completed) {
                        append(" ✓")
                        if (exercise.painLevel > 0) {
                            append(" (Pain: ${exercise.painLevel}/5)")
                        }
                        if (exercise.comments.isNotEmpty()) {
                            append(" - ${exercise.comments}")
                        }
                    } else {
                        append(" ✗")
                    }
                    append("\n")
                }
            } else {
                append("No exercises assigned for this $period.\n")
            }

            append("\nProvide analysis covering:\n")
            append("1. $period performance assessment\n")
            append("2. Progress trends and patterns\n")
            append("3. Exercise compliance evaluation\n")
            append("4. Recommendations for upcoming ${period}s\n")
            append("5. Any concerns or adjustments needed\n")
            append("6. Patient motivation and engagement tips")
        }

        val response = geminiModel.generateContent(prompt)
        val feedbackText = response.text

        if (feedbackText == null) {
            Log.w("PatientDetailsScreen", "Gemini API returned null text")
            return "Unable to generate feedback at this time (null response)."
        }

        Log.i("PatientDetailsScreen", "Successfully generated $period AI feedback")
        return feedbackText
    } catch (e: Exception) {
        Log.e("PatientDetailsScreen", "Error generating $period AI feedback: ${e.message}", e)
        return "Error generating AI feedback: ${e.message}"
    }
}
