package com.naimrlet.rehabmy_test.patient.therapist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.naimrlet.rehabmy_test.chat.ui.ChatScreen

@Composable
fun TherapistScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ChatScreen(onBackClick = onBackClick)
}
