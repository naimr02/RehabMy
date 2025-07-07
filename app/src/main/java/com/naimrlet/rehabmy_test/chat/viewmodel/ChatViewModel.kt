package com.naimrlet.rehabmy_test.chat.viewmodel

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
class ChatViewModel @Inject constructor(
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

    init {
        initializeChat()
    }

    private fun initializeChat() {
        viewModelScope.launch {
            try {
                // Get current user
                chatRepository.getCurrentUser()
                    .onSuccess { user ->
                        _currentUser.value = user
                        findChatPartner()
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

    private fun findChatPartner() {
        viewModelScope.launch {
            chatRepository.findChatPartner()
                .onSuccess { partner ->
                    if (partner != null) {
                        _chatPartner.value = partner
                        loadMessages(partner.id)
                    } else {
                        _messageState.value = MessageState.Success(emptyList())
                    }
                    _isInitialized.value = true
                }
                .onFailure { error ->
                    _messageState.value = MessageState.Error("Failed to find chat partner: ${error.message}")
                    _isInitialized.value = true
                }
        }
    }

    private fun loadMessages(partnerId: String) {
        viewModelScope.launch {
            chatRepository.getMessagesFlow(partnerId)
                .catch { error ->
                    _messageState.value = MessageState.Error("Failed to load messages: ${error.message}")
                }
                .collect { messages ->
                    _messageState.value = MessageState.Success(messages)
                    // Mark messages as read
                    chatRepository.markMessagesAsRead(partnerId)
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

    fun retryChatInitialization() {
        _messageState.value = MessageState.Loading
        _isInitialized.value = false
        initializeChat()
    }
}
