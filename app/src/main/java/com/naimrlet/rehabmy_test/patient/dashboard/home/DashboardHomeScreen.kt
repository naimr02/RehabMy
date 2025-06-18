package com.naimrlet.rehabmy_test.patient.dashboard.home

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.naimrlet.rehabmy_test.model.Exercise
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardHomeScreen(viewModel: ExerciseViewModel = viewModel()) {
    val auth = FirebaseAuth.getInstance()
    val allExercises by viewModel.exercises.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val loading by viewModel.loading.collectAsState()
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var exercisesForSelectedDate by remember { mutableStateOf<List<Exercise>>(emptyList()) }

    // Collect exercises for the selected date
    LaunchedEffect(selectedDate, allExercises) {
        viewModel.exercisesForSelectedDate.collectLatest { filteredExercises ->
            exercisesForSelectedDate = filteredExercises
        }
    }

    LaunchedEffect(Unit) {
        auth.currentUser?.let {
            viewModel.loadExercises()
        } ?: Log.e("AuthCheck", "User not authenticated when entering dashboard")
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your Daily Progress",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            ProgressSection(exercisesForSelectedDate)
            
            // Date selection section
            DateSelectionBar(
                selectedDate = selectedDate,
                onPreviousDay = {
                    val calendar = Calendar.getInstance().apply { time = selectedDate }
                    calendar.add(Calendar.DAY_OF_MONTH, -1)
                    viewModel.setSelectedDate(calendar.time)
                },
                onNextDay = {
                    val calendar = Calendar.getInstance().apply { time = selectedDate }
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    viewModel.setSelectedDate(calendar.time)
                },
                onDateClick = { showDatePicker = true }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ExercisesSection(
                exercises = exercisesForSelectedDate,
                onExerciseClick = { exercise ->
                    selectedExercise = exercise
                }
            )
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { date ->
                viewModel.setSelectedDate(date)
                showDatePicker = false
            },
            initialDate = selectedDate
        )
    }

    selectedExercise?.let { exercise ->
        ExerciseDetailDialog(
            exercise = exercise,
            onDismiss = { selectedExercise = null },
            viewModel = viewModel
        )
    }
}

@Composable
fun DateSelectionBar(
    selectedDate: Date,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateClick: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Previous day"
                )
            }
            
            Row(
                modifier = Modifier
                    .clickable(onClick = onDateClick)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormatter.format(selectedDate),
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select date",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            IconButton(onClick = onNextDay) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Next day"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (Date) -> Unit,
    initialDate: Date
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.time
    )
    
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Date(millis))
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun ExercisesSection(
    exercises: List<Exercise>,
    onExerciseClick: (Exercise) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Today's Exercises",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (exercises.isEmpty()) {
            Text(
                text = "No exercises assigned for this day",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(exercises) { exercise ->
                    ExerciseItem(
                        exercise = exercise,
                        onClick = { onExerciseClick(exercise) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseItem(exercise: Exercise, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (exercise.completed)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 16.dp)
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (exercise.repetitions > 0) {
                    Text(
                        text = "${exercise.repetitions} repetitions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Display pain level if exercise is completed and pain level is recorded
                if (exercise.completed && exercise.painLevel > 0) {
                    Text(
                        text = "Pain Level: ${exercise.painLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            if (exercise.completed) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressSection(exercises: List<Exercise>) {
    val completedCount = exercises.count { it.completed }
    val totalCount = exercises.size
    
    // Calculate percentage or set to 0 if no exercises
    val percentage = if (totalCount > 0) {
        (completedCount.toFloat() / totalCount) * 100f
    } else {
        0f
    }
    
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = tween(1000),
        label = "ProgressAnimation"
    )

    Box(
        modifier = Modifier.size(220.dp).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { animatedPercentage },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 12.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            color = MaterialTheme.colorScheme.primary
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${percentage.toInt()}%",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (totalCount > 0) {
                Text(
                    text = "$completedCount of $totalCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "exercises completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "No exercises",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "for this day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
