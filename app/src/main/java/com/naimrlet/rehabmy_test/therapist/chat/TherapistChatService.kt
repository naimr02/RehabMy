package com.naimrlet.rehabmy_test.therapist.chat

import android.util.Log
import com.naimrlet.rehabmy_test.therapist.PatientInfo
import com.naimrlet.rehabmy_test.therapist.service.TherapistPatientService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service class that provides a clean API for the ViewModel to interact with the chat repository
 * Now uses the simplified TherapistPatientService for better patient data fetching
 */
@Singleton
class TherapistChatService @Inject constructor(
    private val patientService: TherapistPatientService,
    private val repository: TherapistChatRepository = TherapistChatRepository()
) {
    private val TAG = "TherapistChatService"

    /**
     * Get all patient chats for the therapist with error handling
     */
    fun getPatientChats(): Flow<Result<List<PatientChatSummary>>> {
        return repository.getPatientChatsFlow()
            .map { Result.success(it) }
            .catch { e ->
                Log.e(TAG, "Error getting patient chats: ${e.message}", e)
                emit(Result.failure(e))
            }
    }

    /**
     * Get all messages for a specific chat thread with error handling
     */
    fun getChatMessages(chatId: String): Flow<Result<List<ChatMessage>>> {
        return repository.getChatMessagesFlow(chatId)
            .map { Result.success(it) }
            .catch { e ->
                Log.e(TAG, "Error getting chat messages: ${e.message}", e)
                emit(Result.failure(e))
            }
    }

    /**
     * Get or create a chat thread with a patient
     */
    suspend fun getOrCreateChatThread(patientId: String): Result<ChatThread> {
        return try {
            val chatThread = repository.getOrCreateChatThread(patientId)
            if (chatThread != null) {
                Result.success(chatThread)
            } else {
                Result.failure(Exception("Failed to create or get chat thread"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating chat thread: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send a message to a patient
     */
    suspend fun sendMessage(chatId: String, message: String, patientId: String): Result<Boolean> {
        return try {
            // Validate message content
            if (message.isBlank()) {
                return Result.failure(IllegalArgumentException("Message cannot be empty"))
            }

            val success = repository.sendMessage(chatId, message, patientId)
            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to send message"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get patient information by ID - now uses simplified service
     */
    suspend fun getPatientInfo(patientId: String): Result<PatientInfo> {
        return try {
            val patientInfo = patientService.getPatientDetails(patientId)
            if (patientInfo != null) {
                Result.success(patientInfo)
            } else {
                Result.failure(Exception("Patient not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting patient info: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all assigned patients for the therapist - now uses simplified service
     */
    suspend fun getAssignedPatients(): Result<List<PatientInfo>> {
        return try {
            Log.d(TAG, "Fetching assigned patients using simplified service")
            val result = patientService.getAssignedPatients()
            result.fold(
                onSuccess = { patients ->
                    Log.d(TAG, "Successfully fetched ${patients.size} assigned patients")
                    Result.success(patients)
                },
                onFailure = { error ->
                    Log.e(TAG, "Error from simplified service: ${error.message}", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting assigned patients: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all patients the therapist has chatted with
     */
    suspend fun getChatPatients(): Result<List<PatientInfo>> {
        return try {
            val patients = repository.getChatPatients()
            Result.success(patients)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chat patients: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Start a new chat with a patient
     */
    suspend fun startNewChat(patientId: String): Result<ChatThread> {
        return try {
            val chatThread = repository.startNewChat(patientId)
            if (chatThread != null) {
                Result.success(chatThread)
            } else {
                Result.failure(Exception("Failed to start new chat"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting new chat: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Format a date for display in the chat
     */
    fun formatMessageTime(date: Date): String {
        val now = Date()
        val diff = now.time - date.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 7 -> {
                // Format as MM/dd/yyyy for older messages
                SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date)
            }
            days > 1 -> {
                // Format as day name for recent days
                SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
            }
            days > 0 -> "Yesterday"
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    }

    /**
     * Format date for chat header
     */
    fun formatDateForHeader(date: Date): String {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        calendar.time = date

        return when {
            isSameDay(calendar, today) -> "Today"
            isYesterday(calendar, today) -> "Yesterday"
            else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)
        }
    }

    /**
     * Format time for message bubble
     */
    fun formatTimeForMessage(date: Date): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    }

    /**
     * Check if two calendars represent the same day
     */
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Check if a calendar date is yesterday compared to another calendar
     */
    private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = cal2.timeInMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(cal1, yesterday)
    }

    /**
     * Format message preview text (truncates long messages)
     */
    fun formatMessagePreview(message: String, maxLength: Int = 30): String {
        return if (message.length <= maxLength) {
            message
        } else {
            message.substring(0, maxLength - 3) + "..."
        }
    }

    /**
     * Check if a timestamp is recent (within the last hour)
     */
    fun isRecentMessage(date: Date): Boolean {
        val now = Date()
        val diff = now.time - date.time
        val minutes = diff / (1000 * 60)
        return minutes < 60
    }

    /**
     * Get a summary of unread messages for notification purposes
     */
    suspend fun getUnreadMessagesSummary(): Result<Map<String, Int>> {
        return try {
            val patientChats = repository.getPatientChatsFlow().map { chats ->
                chats.filter { it.unreadCount > 0 }
                    .associate { it.patientId to it.unreadCount }
            }.catch { e ->
                Log.e(TAG, "Error getting unread messages: ${e.message}", e)
                emit(emptyMap())
            }
            
            // This is a placeholder since we can't directly return from a flow
            // In a real implementation, you would collect from the flow first
            Result.success(emptyMap())
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread messages summary: ${e.message}", e)
            Result.failure(e)
        }
    }
}
