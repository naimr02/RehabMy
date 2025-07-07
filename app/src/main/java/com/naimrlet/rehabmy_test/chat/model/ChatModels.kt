package com.naimrlet.rehabmy_test.chat.model

data class ChatUser(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val isTherapist: Boolean = false,
    val isOnline: Boolean = false
)

data class ConversationPreview(
    val conversationId: String = "",
    val otherUser: ChatUser = ChatUser(),
    val lastMessage: ChatMessage? = null,
    val unreadCount: Int = 0,
    val lastActivity: Long = 0L
)

sealed class ChatState {
    object Loading : ChatState()
    data class Success(
        val conversations: List<ConversationPreview> = emptyList(),
        val selectedConversation: String? = null
    ) : ChatState()
    data class Error(val message: String) : ChatState()
}

sealed class MessageState {
    object Loading : MessageState()
    data class Success(val messages: List<ChatMessage> = emptyList()) : MessageState()
    data class Error(val message: String) : MessageState()
}

data class MessageInputState(
    val text: String = "",
    val isLoading: Boolean = false
)
