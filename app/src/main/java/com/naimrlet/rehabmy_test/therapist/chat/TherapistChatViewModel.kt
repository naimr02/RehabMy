package com.naimrlet.rehabmy_test.therapist.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naimrlet.rehabmy_test.therapist.PatientInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for the therapist's chat functionality
 * Now uses Hilt dependency injection for better service management
 */
@HiltViewModel
class TherapistChatViewModel @Inject constructor(
    private val chatService: TherapistChatService
) : ViewModel() {
    private val TAG = "TherapistChatViewModel"

    // State for the patient chat list
    private val _chatListState = MutableStateFlow(TherapistChatState())
    val chatListState: StateFlow<TherapistChatState> = _chatListState.asStateFlow()

    // State for the current active conversation
    private val _conversationState = MutableStateFlow(ChatConversationState())
    val conversationState: StateFlow<ChatConversationState> = _conversationState.asStateFlow()

    // State for the message input field
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    // Keep track of currently selected chat thread
    private var currentChatId: String? = null
    private var currentPatientId: String? = null

    init {
        // Load patient chats when ViewModel is created
        loadPatientChats()
    }

    /**
     * Load all patient chats for the therapist
     */
    fun loadPatientChats() {
        viewModelScope.launch {
            _chatListState.update { it.copy(isLoading = true, error = null) }
            
            viewModelScope.launch {
                chatService.getPatientChats().collectLatest { result ->
                    result.fold(
                        onSuccess = { chats ->
                            _chatListState.update { 
                                it.copy(
                                    isLoading = false,
                                    patientChats = chats,
                                    error = null
                                )
                            }
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Error loading patient chats: ${error.message}", error)
                            _chatListState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Failed to load chats: ${error.message}"
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    /**
     * Select a patient to chat with
     */
    fun selectPatient(patientId: String) {
        viewModelScope.launch {
            _chatListState.update { it.copy(selectedPatientId = patientId) }
            
            // Create or get chat thread for this patient
            val threadResult = chatService.getOrCreateChatThread(patientId)
            threadResult.fold(
                onSuccess = { thread ->
                    currentChatId = thread.id
                    currentPatientId = patientId
                    
                    // Load patient information
                    loadPatientInfo(patientId)
                    
                    // Load messages for this chat
                    loadChatMessages(thread.id)
                },
                onFailure = { error ->
                    Log.e(TAG, "Error selecting patient: ${error.message}", error)
                    _conversationState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load conversation: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Load messages for a specific chat thread
     */
    private fun loadChatMessages(chatId: String) {
        viewModelScope.launch {
            _conversationState.update { it.copy(isLoading = true, error = null) }
            
            viewModelScope.launch {
                chatService.getChatMessages(chatId).collectLatest { result ->
                    result.fold(
                        onSuccess = { messages ->
                            _conversationState.update { 
                                it.copy(
                                    isLoading = false,
                                    messages = messages,
                                    error = null
                                )
                            }
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Error loading chat messages: ${error.message}", error)
                            _conversationState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Failed to load messages: ${error.message}"
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    /**
     * Load patient information
     */
    private fun loadPatientInfo(patientId: String) {
        viewModelScope.launch {
            val result = chatService.getPatientInfo(patientId)
            result.fold(
                onSuccess = { patient ->
                    _conversationState.update { 
                        it.copy(patient = patient) 
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Error loading patient info: ${error.message}", error)
                }
            )
        }
    }

    /**
     * Update the message input text
     */
    fun updateMessageText(text: String) {
        _messageText.value = text
    }

    /**
     * Send a message to the current patient
     */
    fun sendMessage() {
        val messageText = _messageText.value.trim()
        val chatId = currentChatId
        val patientId = currentPatientId
        
        if (messageText.isBlank() || chatId == null || patientId == null) {
            return
        }
        
        viewModelScope.launch {
            _conversationState.update { it.copy(isSubmitting = true) }
            
            // Clear message input immediately for better UX
            _messageText.value = ""
            
            val result = chatService.sendMessage(chatId, messageText, patientId)
            
            result.fold(
                onSuccess = {
                    _conversationState.update { it.copy(isSubmitting = false) }
                    // No need to reload messages as the listener will handle this
                },
                onFailure = { error ->
                    Log.e(TAG, "Error sending message: ${error.message}", error)
                    _conversationState.update {
                        it.copy(
                            isSubmitting = false,
                            error = "Failed to send message: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Send an exercise to the patient via chat
     */
    fun sendExerciseMessage(exerciseId: String) {
        val chatId = currentChatId
        val patientId = currentPatientId
        
        if (chatId == null || patientId == null) {
            return
        }
        
        viewModelScope.launch {
            val exerciseMessage = "I've assigned you a new exercise. Please check your exercises tab."
            chatService.sendMessage(chatId, exerciseMessage, patientId)
        }
    }

    /**
     * Clear the current conversation and go back to the chat list
     */
    fun clearConversation() {
        currentChatId = null
        currentPatientId = null
        _chatListState.update { it.copy(selectedPatientId = null) }
        _conversationState.update { 
            ChatConversationState(isLoading = false) 
        }
    }

    /**
     * Format the date for display in the chat list
     */
    fun formatLastMessageTime(date: Date?): String {
        if (date == null) return ""
        return chatService.formatMessageTime(date)
    }

    /**
     * Determine if messages should show with a date header
     */
    fun shouldShowDateHeader(index: Int, messages: List<ChatMessage>): Boolean {
        if (index == 0) return true
        
        val currentMessageDate = messages[index].timestamp
        val previousMessageDate = messages[index - 1].timestamp
        
        val currentCalendar = java.util.Calendar.getInstance().apply { time = currentMessageDate }
        val previousCalendar = java.util.Calendar.getInstance().apply { time = previousMessageDate }
        
        return currentCalendar.get(java.util.Calendar.DAY_OF_YEAR) != previousCalendar.get(java.util.Calendar.DAY_OF_YEAR) ||
                currentCalendar.get(java.util.Calendar.YEAR) != previousCalendar.get(java.util.Calendar.YEAR)
    }

    /**
     * Refresh all data
     */
    fun refresh() {
        loadPatientChats()
        currentChatId?.let { loadChatMessages(it) }
        currentPatientId?.let { loadPatientInfo(it) }
    }
}
