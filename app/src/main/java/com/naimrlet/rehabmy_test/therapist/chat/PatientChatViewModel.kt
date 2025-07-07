package com.naimrlet.rehabmy_test.therapist.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naimrlet.rehabmy_test.chat.model.ChatMessage
import com.naimrlet.rehabmy_test.chat.model.ChatUser
import com.naimrlet.rehabmy_test.chat.model.MessageInputState
import com.naimrlet.rehabmy_test.chat.model.MessageState
import com.naimrlet.rehabmy_test.chat.model.UserType
import com.naimrlet.rehabmy_test.chat.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<ChatUser?>(null)
    val currentUser: StateFlow<ChatUser?> = _currentUser.asStateFlow()

    private val _chatPartner = MutableStateFlow<ChatUser?>(null)
    val chatPartner: StateFlow<ChatUser?> = _chatPartner.asStateFlow()

    private val _messageState = MutableStateFlow<MessageState>(MessageState.Loading)
    val messageState: StateFlow<MessageState> = _messageState.asStateFlow()

    private val _messageInputState = MutableStateFlow(MessageInputState())
    val messageInputState: StateFlow<MessageInputState> = _messageInputState.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /**
     * Initialize chat with a specific patient
     */
    fun initializeChatWithPatient(patientId: String) {
        viewModelScope.launch {
            try {
                _messageState.value = MessageState.Loading
                _isInitialized.value = false

                // Get current user (therapist)
                chatRepository.getCurrentUser()
                    .onSuccess { user ->
                        _currentUser.value = user
                        loadSpecificPatient(patientId)
                    }
                    .onFailure { error ->
                        _messageState.value = MessageState.Error("Failed to load user: ${error.message}")
                        _isInitialized.value = true
                    }
            } catch (e: Exception) {
                _messageState.value = MessageState.Error("Failed to initialize chat: ${e.message}")
                _isInitialized.value = true
            }
        }
    }

    private fun loadSpecificPatient(patientId: String) {
        viewModelScope.launch {
            try {
                val patient = chatRepository.getUserById(patientId)
                if (patient != null) {
                    _chatPartner.value = patient
                    loadMessages(patientId)
                } else {
                    _messageState.value = MessageState.Error("Patient not found")
                }
                _isInitialized.value = true
            } catch (e: Exception) {
                _messageState.value = MessageState.Error("Failed to load patient: ${e.message}")
                _isInitialized.value = true
            }
        }
    }

    private fun loadMessages(patientId: String) {
        viewModelScope.launch {
            chatRepository.getMessagesFlow(patientId)
                .catch { error ->
                    _messageState.value = MessageState.Error("Failed to load messages: ${error.message}")
                }
                .collect { messages ->
                    _messageState.value = MessageState.Success(messages)
                    // Mark messages as read
                    chatRepository.markMessagesAsRead(patientId)
                }
        }
    }

    fun updateMessageText(text: String) {
        _messageInputState.value = _messageInputState.value.copy(text = text)
    }

    fun sendMessage() {
        val messageText = _messageInputState.value.text.trim()
        val partner = _chatPartner.value
        val user = _currentUser.value

        if (messageText.isBlank() || partner == null || user == null) return

        viewModelScope.launch {
            _messageInputState.value = _messageInputState.value.copy(isLoading = true)

            val senderType = if (user.isTherapist) UserType.THERAPIST else UserType.PATIENT

            chatRepository.sendMessage(
                recipientId = partner.id,
                content = messageText,
                senderName = user.name,
                senderType = senderType
            )
                .onSuccess {
                    _messageInputState.value = MessageInputState() // Clear input
                }
                .onFailure { error ->
                    _messageInputState.value = _messageInputState.value.copy(
                        isLoading = false
                    )
                    // You could show an error message here
                }
        }
    }

    fun refreshMessages() {
        _chatPartner.value?.let { partner ->
            loadMessages(partner.id)
        }
    }

    fun retryChatInitialization(patientId: String) {
        initializeChatWithPatient(patientId)
    }
}
