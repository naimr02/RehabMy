package com.naimrlet.rehabmy_test.therapist.chat

/**
 * Events that can be triggered from the UI
 */
sealed class TherapistChatUIEvent {
    data class SelectPatient(val patientId: String) : TherapistChatUIEvent()
    data class UpdateMessageText(val text: String) : TherapistChatUIEvent()
    object SendMessage : TherapistChatUIEvent()
    data class SendExercise(val exerciseId: String) : TherapistChatUIEvent()
    object BackToList : TherapistChatUIEvent()
    object RefreshData : TherapistChatUIEvent()
    data class ShowPatientProfile(val patientId: String) : TherapistChatUIEvent()
}

/**
 * Helper class to handle UI events in composables
 */
class TherapistChatUIEventHandler(private val viewModel: TherapistChatViewModel) {
    
    fun handleEvent(event: TherapistChatUIEvent) {
        when (event) {
            is TherapistChatUIEvent.SelectPatient -> {
                viewModel.selectPatient(event.patientId)
            }
            
            is TherapistChatUIEvent.UpdateMessageText -> {
                viewModel.updateMessageText(event.text)
            }
            
            is TherapistChatUIEvent.SendMessage -> {
                viewModel.sendMessage()
            }
            
            is TherapistChatUIEvent.SendExercise -> {
                viewModel.sendExerciseMessage(event.exerciseId)
            }
            
            is TherapistChatUIEvent.BackToList -> {
                viewModel.clearConversation()
            }
            
            is TherapistChatUIEvent.RefreshData -> {
                viewModel.refresh()
            }
            
            is TherapistChatUIEvent.ShowPatientProfile -> {
                // This would typically navigate to a patient profile screen
                // For now, we'll just select the patient for chat
                viewModel.selectPatient(event.patientId)
            }
        }
    }
}
