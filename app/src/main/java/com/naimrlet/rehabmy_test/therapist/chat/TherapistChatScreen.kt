package com.naimrlet.rehabmy_test.therapist.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Updated Therapist chat screen - manages navigation between patient list and individual chat
 */
@Composable
fun TherapistChatScreen(
    onBackClick: () -> Unit = {},
    onPatientSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // State to track which screen to show
    var selectedPatientId by remember { mutableStateOf<String?>(null) }

    if (selectedPatientId != null) {
        // Show individual patient chat
        PatientChatScreen(
            patientId = selectedPatientId!!,
            onBackClick = {
                // Go back to patient list
                selectedPatientId = null
            },
            modifier = modifier
        )
    } else {
        // Show patient list
        TherapistChatListScreen(
            onBackClick = onBackClick,
            onPatientSelected = { patientId ->
                selectedPatientId = patientId
                onPatientSelected(patientId) // Also call the external callback
            }
        )
    }
}
