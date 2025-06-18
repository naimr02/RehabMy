package com.naimrlet.rehabmy_test.therapist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
                text = "${exercise.duration} min, ${exercise.frequency}",
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
                progress = { exercise.completion },
                modifier = Modifier.width(80.dp),
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "${(exercise.completion * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Display patient feedback if available
        if (exercise.completion > 0 && exercise.painLevel > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Patient reported pain level: ${exercise.painLevel}/5",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )

            if (exercise.comments.isNotEmpty()) {
                Text(
                    text = "Comments: ${exercise.comments}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}