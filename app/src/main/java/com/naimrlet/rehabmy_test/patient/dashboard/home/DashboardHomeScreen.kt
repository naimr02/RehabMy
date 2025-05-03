package com.naimrlet.rehabmy_test.patient.dashboard.home

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun DashboardHomeScreen(viewModel: ExerciseViewModel = viewModel()) {
    val auth = FirebaseAuth.getInstance()
    val exercises by viewModel.exercises.collectAsState()
    val loading by viewModel.loading.collectAsState()

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
            text = "Your Progress",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            ProgressSection(exercises)
            Spacer(modifier = Modifier.height(32.dp))
            ExercisesSection(
                exercises = exercises,
                onExerciseCheckedChange = { exercise, isCompleted ->
                    viewModel.updateExerciseStatus(exercise, isCompleted)
                }
            )
        }
    }
}

@Composable
private fun ExercisesSection(
    exercises: List<Exercise>,
    onExerciseCheckedChange: (Exercise, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Your Exercises",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (exercises.isEmpty()) {
            Text(
                text = "No exercises assigned yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(exercises) { exercise ->
                    ExerciseItem(
                        exercise = exercise,
                        onCheckedChange = { isChecked ->
                            onExerciseCheckedChange(exercise, isChecked)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressSection(exercises: List<Exercise>) {
    val completedCount = exercises.count { it.completed }
    val totalCount = exercises.size.coerceAtLeast(1)
    val percentage = (completedCount.toFloat() / totalCount) * 100f
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
        }
    }
}

@Composable
fun ExerciseItem(exercise: Exercise, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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
            }
            Checkbox(
                checked = exercise.completed,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}
