package com.naimrlet.rehabmy_test.chat.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ChatMessage(
    @DocumentId
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderType: UserType = UserType.PATIENT,
    val content: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val isRead: Boolean = false,
    val messageType: MessageType = MessageType.TEXT
)

enum class MessageType {
    TEXT,
    IMAGE,
    FILE,
    SYSTEM
}

enum class UserType {
    PATIENT,
    THERAPIST
}
