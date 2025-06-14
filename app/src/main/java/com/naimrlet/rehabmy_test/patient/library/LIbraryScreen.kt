package com.naimrlet.rehabmy_test.patient.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naimrlet.rehabmy_test.model.BodyPart
import com.naimrlet.rehabmy_test.model.LibraryExercise
import java.util.Locale

@Composable
fun LibraryScreen(viewModel: LibraryViewModel = viewModel()) {

    val loading by viewModel.loading.collectAsState()
    val selectedBodyPart by viewModel.selectedBodyPart.collectAsState()
    var selectedExercise by remember { mutableStateOf<LibraryExercise?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            selectedExercise != null -> {
                ExerciseDetailScreen(
                    exercise = selectedExercise!!,
                    onBack = { selectedExercise = null }
                )
            }
            selectedBodyPart != null -> {
                BodyPartExercisesScreen(
                    bodyPart = selectedBodyPart!!,
                    exercises = viewModel.getExercisesByBodyPart(selectedBodyPart!!),
                    onExerciseClick = { selectedExercise = it },
                    onBack = { viewModel.clearSelection() }
                )
            }
            else -> {
                BodyPartCategoriesScreen(
                    onBodyPartClick = { viewModel.selectBodyPart(it) }
                )
            }
        }
    }
}

@Composable
fun BodyPartCategoriesScreen(onBodyPartClick: (BodyPart) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Exercise Library",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        Text(
            text = "Select a body part to view exercises",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(BodyPart.entries.toTypedArray()) { bodyPart ->
                BodyPartCard(bodyPart = bodyPart, onClick = { onBodyPartClick(bodyPart) })
            }
        }
    }
}

@Composable
fun BodyPartCard(bodyPart: BodyPart, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getBodyPartIcon(bodyPart),
                contentDescription = bodyPart.name,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = bodyPart.name.lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BodyPartExercisesScreen(
    bodyPart: BodyPart,
    exercises: List<LibraryExercise>,
    onExerciseClick: (LibraryExercise) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to categories"
                )
            }
            Text(
                text = "${bodyPart.name.lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }} Exercises",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No exercises available for this body part",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

@Composable
fun ExerciseItem(exercise: LibraryExercise, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = exercise.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun getBodyPartIcon(bodyPart: BodyPart): ImageVector {
    return when (bodyPart) {
        BodyPart.HIP -> Icons.Default.FitnessCenter
        BodyPart.KNEE -> Icons.Default.AccessibilityNew
        BodyPart.ANKLE -> Icons.AutoMirrored.Filled.DirectionsWalk
        BodyPart.FOOT -> Icons.AutoMirrored.Filled.DirectionsRun
    }
}
