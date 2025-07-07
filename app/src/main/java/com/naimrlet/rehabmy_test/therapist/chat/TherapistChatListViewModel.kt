package com.naimrlet.rehabmy_test.therapist.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.naimrlet.rehabmy_test.chat.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TherapistChatListViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val chatRepository: ChatRepository
) : ViewModel() {

    companion object {
        private const val TAG = "TherapistChatListVM"
        private const val USERS_COLLECTION = "users"
        private const val CONVERSATIONS_COLLECTION = "conversations"
        private const val MESSAGES_COLLECTION = "messages"
    }

    private val _patients = MutableStateFlow<List<PatientChatInfo>>(emptyList())
    val patients: StateFlow<List<PatientChatInfo>> = _patients.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun loadPatients() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val currentUserId = getCurrentUserId()
                if (currentUserId == null) {
                    _error.value = "User not authenticated"
                    _isLoading.value = false
                    return@launch
                }

                // Get therapist's assigned patients
                val therapistDoc = firestore.collection(USERS_COLLECTION)
                    .document(currentUserId)
                    .get()
                    .await()

                if (!therapistDoc.exists()) {
                    _error.value = "Therapist profile not found"
                    _isLoading.value = false
                    return@launch
                }

                val assignedPatientIds = therapistDoc.get("assignedPatient") as? List<String> ?: emptyList()

                if (assignedPatientIds.isEmpty()) {
                    _patients.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                // Load patient details and chat info
                val patientChatInfos = assignedPatientIds.mapNotNull { patientId ->
                    try {
                        loadPatientChatInfo(currentUserId, patientId)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load chat info for patient $patientId: ${e.message}")
                        null
                    }
                }

                // Sort patients by last message time (most recent first)
                val sortedPatients = patientChatInfos.sortedByDescending {
                    it.lastMessageTime.ifEmpty { "0" }
                }

                _patients.value = sortedPatients
                _isLoading.value = false

            } catch (e: Exception) {
                Log.e(TAG, "Error loading patients", e)
                _error.value = "Failed to load patients: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadPatientChatInfo(therapistId: String, patientId: String): PatientChatInfo {
        // Get patient basic info
        val patientDoc = firestore.collection(USERS_COLLECTION)
            .document(patientId)
            .get()
            .await()

        val patientName = patientDoc.getString("name") ?: "Unknown Patient"
        val profileImageUrl = patientDoc.getString("profileImageUrl") ?: ""

        // Use patientId directly as conversation document ID to match conversations/{patientId}/messages structure
        val conversationId = patientId

        // Get last message and unread count
        val (lastMessage, lastMessageTime, unreadCount) = getLastMessageInfo(conversationId, therapistId)

        return PatientChatInfo(
            id = patientId,
            name = patientName,
            profileImageUrl = profileImageUrl,
            lastMessage = lastMessage,
            lastMessageTime = lastMessageTime,
            unreadCount = unreadCount,
            isOnline = false // Can be enhanced later with presence detection
        )
    }

    private suspend fun getLastMessageInfo(conversationId: String, currentUserId: String): Triple<String, String, Int> {
        return try {
            // Get the last message using the conversations/{patientId}/messages structure
            val lastMessageQuery = firestore.collection(CONVERSATIONS_COLLECTION)
                .document(conversationId) // This is the patientId
                .collection(MESSAGES_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val lastMessage = if (!lastMessageQuery.isEmpty) {
                val messageDoc = lastMessageQuery.documents.first()
                val content = messageDoc.getString("content") ?: ""
                val timestamp = messageDoc.getDate("timestamp")
                val senderId = messageDoc.getString("senderId") ?: ""

                val timeString = timestamp?.let { formatMessageTime(it) } ?: ""
                val messagePrefix = if (senderId == currentUserId) "You: " else ""

                Triple(messagePrefix + content, timeString, 0)
            } else {
                Triple("", "", 0)
            }

            // For now, set unread count to 0 to avoid the composite index requirement
            // TODO: Implement unread count with proper indexing strategy later
            val unreadCount = 0

            Triple(lastMessage.first, lastMessage.second, unreadCount)

        } catch (e: Exception) {
            Log.w(TAG, "Error getting last message info for conversation $conversationId: ${e.message}")
            Triple("", "", 0)
        }
    }

    private fun formatMessageTime(timestamp: Date): String {
        val now = Date()
        val diff = now.time - timestamp.time

        return when {
            diff < 60_000 -> "Just now" // Less than 1 minute
            diff < 3600_000 -> "${diff / 60_000}m ago" // Less than 1 hour
            diff < 86400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp) // Same day
            diff < 604800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(timestamp) // This week
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(timestamp) // Older
        }
    }

    fun refreshPatients() {
        loadPatients()
    }
}
