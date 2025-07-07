package com.naimrlet.rehabmy_test.therapist.chat

import com.google.firebase.Timestamp
import com.naimrlet.rehabmy_test.therapist.PatientInfo
import java.util.Date

/**
 * Represents a single chat message in the conversation
 */
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderType: SenderType = SenderType.THERAPIST,
    val message: String = "",
    val timestamp: Date = Date(),
    val isRead: Boolean = false,
    val attachmentUrl: String? = null
)

/**
 * Identifies the type of sender for a message
 */
enum class SenderType {
    PATIENT,
    THERAPIST
}

/**
 * Represents a conversation between a therapist and a patient
 */
data class ChatThread(
    val id: String = "",
    val patientId: String = "",
    val therapistId: String = "",
    val lastMessage: ChatMessage? = null,
    val unreadCount: Int = 0,
    val lastActivity: Date = Date()
)

/**
 * Summary information for displaying patient chat in therapist's inbox
 */
data class PatientChatSummary(
    val patientId: String,
    val patientName: String,
    val patientImageUrl: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: Date? = null,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val condition: String? = null
)

/**
 * Represents the state of the therapist's chat list
 */
data class TherapistChatState(
    val isLoading: Boolean = true,
    val patientChats: List<PatientChatSummary> = emptyList(),
    val selectedPatientId: String? = null,
    val error: String? = null
)

/**
 * Represents the state of an individual chat conversation
 */
data class ChatConversationState(
    val isLoading: Boolean = true,
    val messages: List<ChatMessage> = emptyList(),
    val patient: PatientInfo? = null,
    val error: String? = null,
    val isSubmitting: Boolean = false
)
