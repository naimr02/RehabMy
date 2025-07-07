package com.naimrlet.rehabmy_test.therapist.chat

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.naimrlet.rehabmy_test.therapist.PatientInfo
import com.naimrlet.rehabmy_test.therapist.ExerciseInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class to handle Firestore operations for the therapist chat functionality
 */
@Singleton
class TherapistChatRepository @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "TherapistChatRepo"

    // Collection paths
    private val USERS_COLLECTION = "users"
    private val CHATS_COLLECTION = "chats"
    private val MESSAGES_SUBCOLLECTION = "messages"
    private val EXERCISES_SUBCOLLECTION = "exercises"

    /**
     * Get the current therapist ID
     */
    private fun getCurrentTherapistId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Create or get a chat thread between therapist and patient
     */
    suspend fun getOrCreateChatThread(patientId: String): ChatThread? {
        val therapistId = getCurrentTherapistId() ?: return null

        try {
            // Check if chat already exists
            val chatQuery = db.collection(CHATS_COLLECTION)
                .whereEqualTo("therapistId", therapistId)
                .whereEqualTo("patientId", patientId)
                .limit(1)
                .get()
                .await()

            if (!chatQuery.isEmpty) {
                // Chat exists, return it
                val chatDoc = chatQuery.documents[0]
                return mapDocumentToChatThread(chatDoc)
            }

            // Chat doesn't exist, create a new one
            val chatId = UUID.randomUUID().toString()
            val chatData = hashMapOf(
                "id" to chatId,
                "therapistId" to therapistId,
                "patientId" to patientId,
                "lastActivity" to Timestamp.now(),
                "participants" to listOf(therapistId, patientId),
                "unreadCount" to hashMapOf(
                    therapistId to 0,
                    patientId to 0
                )
            )

            // Create the chat document
            db.collection(CHATS_COLLECTION).document(chatId)
                .set(chatData)
                .await()

            return ChatThread(
                id = chatId,
                therapistId = therapistId,
                patientId = patientId,
                lastActivity = Date()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating chat thread: ${e.message}", e)
            return null
        }
    }

    /**
     * Get all patient chats for the current therapist
     */
    fun getPatientChatsFlow(): Flow<List<PatientChatSummary>> = callbackFlow {
        val therapistId = getCurrentTherapistId()
        if (therapistId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Query chats where therapist is a participant
        val chatsQuery = db.collection(CHATS_COLLECTION)
            .whereEqualTo("therapistId", therapistId)
            .orderBy("lastActivity", Query.Direction.DESCENDING)

        val listener = chatsQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to chats: ${error.message}", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            // Process chat documents
            val chatSummaries = mutableListOf<PatientChatSummary>()
            snapshot.documents.forEach { chatDoc ->
                val patientId = chatDoc.getString("patientId") ?: return@forEach

                // Build a chat summary (will fetch patient details asynchronously later)
                val lastMessage = chatDoc.get("lastMessage") as? String
                val lastMessageTime = (chatDoc.get("lastActivity") as? Timestamp)?.toDate()
                val unreadCountMap = chatDoc.get("unreadCount") as? Map<String, Long>
                val unreadCount = unreadCountMap?.get(therapistId)?.toInt() ?: 0

                chatSummaries.add(
                    PatientChatSummary(
                        patientId = patientId,
                        patientName = "Loading...", // Will be updated with patient data
                        lastMessage = lastMessage,
                        lastMessageTime = lastMessageTime,
                        unreadCount = unreadCount
                    )
                )
            }

            // Send initial list with basic data
            trySend(chatSummaries)
            
            // Then fetch and update patient details
            chatSummaries.forEach { summary ->
                try {
                    db.collection(USERS_COLLECTION).document(summary.patientId)
                        .get()
                        .addOnSuccessListener { patientDoc ->
                            if (patientDoc != null && patientDoc.exists()) {
                                val updatedSummary = summary.copy(
                                    patientName = patientDoc.getString("name") ?: "Unknown",
                                    condition = patientDoc.getString("condition"),
                                    patientImageUrl = patientDoc.getString("profileImageUrl")
                                )
                                
                                // Update the specific summary in our list
                                val updatedList = chatSummaries.map { 
                                    if (it.patientId == updatedSummary.patientId) updatedSummary else it 
                                }
                                
                                // Send updated list
                                trySend(updatedList)
                            }
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching patient details: ${e.message}", e)
                }
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Get messages for a specific chat thread
     */
    fun getChatMessagesFlow(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val therapistId = getCurrentTherapistId()
        if (therapistId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Mark messages as read when opening the chat
        markChatAsRead(chatId, therapistId)

        // Query messages for this chat, ordered by timestamp
        val messagesQuery = db.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_SUBCOLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val listener = messagesQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to messages: ${error.message}", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val messages = snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val senderId = doc.getString("senderId") ?: return@mapNotNull null
                    val senderName = doc.getString("senderName") ?: ""
                    val senderTypeStr = doc.getString("senderType") ?: SenderType.THERAPIST.name
                    val senderType = try {
                        SenderType.valueOf(senderTypeStr)
                    } catch (e: Exception) {
                        SenderType.THERAPIST
                    }
                    val message = doc.getString("message") ?: ""
                    val timestamp = (doc.get("timestamp") as? Timestamp)?.toDate() ?: Date()
                    val isRead = doc.getBoolean("isRead") ?: false
                    val attachmentUrl = doc.getString("attachmentUrl")

                    ChatMessage(
                        id = id,
                        senderId = senderId,
                        senderName = senderName,
                        senderType = senderType,
                        message = message,
                        timestamp = timestamp,
                        isRead = isRead,
                        attachmentUrl = attachmentUrl
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message: ${e.message}", e)
                    null
                }
            }

            trySend(messages)

            // Mark new messages as read
            markNewMessagesAsRead(chatId, messages, therapistId)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Send a message in a chat thread
     */
    suspend fun sendMessage(chatId: String, message: String, patientId: String): Boolean {
        val therapistId = getCurrentTherapistId() ?: return false
        
        try {
            // Get therapist name
            val therapistDoc = db.collection(USERS_COLLECTION)
                .document(therapistId)
                .get()
                .await()
                
            val therapistName = therapistDoc.getString("name") ?: "Therapist"
            
            // Create message document
            val messageId = UUID.randomUUID().toString()
            val now = Timestamp.now()
            
            val messageData = hashMapOf(
                "id" to messageId,
                "senderId" to therapistId,
                "senderName" to therapistName,
                "senderType" to SenderType.THERAPIST.name,
                "message" to message,
                "timestamp" to now,
                "isRead" to false
            )
            
            // Add message to subcollection
            db.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_SUBCOLLECTION)
                .document(messageId)
                .set(messageData)
                .await()
                
            // Update chat thread with last message info
            val chatUpdates = hashMapOf(
                "lastMessage" to message,
                "lastActivity" to now,
                "unreadCount.${patientId}" to FieldValue.increment(1)
            )
            
            db.collection(CHATS_COLLECTION)
                .document(chatId)
                .set(chatUpdates, SetOptions.merge())
                .await()
                
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}", e)
            return false
        }
    }

    /**
     * Get patient information by ID
     */
    suspend fun getPatientInfo(patientId: String): PatientInfo? {
        try {
            val patientDoc = db.collection(USERS_COLLECTION)
                .document(patientId)
                .get()
                .await()
                
            if (!patientDoc.exists()) return null

            // Get patient exercises
            val exercisesSnapshot = db.collection(USERS_COLLECTION)
                .document(patientId)
                .collection(EXERCISES_SUBCOLLECTION)
                .get()
                .await()

            // Map exercises to ExerciseInfo objects
            val exercises = exercisesSnapshot.documents.mapNotNull { doc ->
                try {
                    ExerciseInfo(
                        id = doc.id,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        description = doc.getString("description") ?: "",
                        duration = doc.getLong("duration")?.toInt() ?: 0,
                        frequency = doc.getString("frequency") ?: "Daily",
                        painLevel = doc.getLong("painLevel")?.toInt() ?: 0,
                        comments = doc.getString("comments") ?: "",
                        dueDate = doc.getDate("dueDate"),
                        completed = doc.getBoolean("completed") ?: false
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // Calculate completion
            val completedExercises = exercises.count { it.completed }
            
            return PatientInfo(
                id = patientId,
                name = patientDoc.getString("name") ?: "Unknown",
                age = patientDoc.getLong("age")?.toInt() ?: 0,
                condition = patientDoc.getString("condition") ?: "Not specified",
                completedExercises = completedExercises,
                totalExercises = exercises.size,
                assignedExercises = exercises
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting patient info: ${e.message}", e)
            return null
        }
    }

    /**
     * Get all patients assigned to the current therapist
     */
    suspend fun getAssignedPatients(): List<PatientInfo> {
        val therapistId = getCurrentTherapistId() ?: return emptyList()
        
        try {
            // Get therapist document to find assigned patients
            val therapistDoc = db.collection(USERS_COLLECTION)
                .document(therapistId)
                .get()
                .await()
                
            // Get the list of assigned patient IDs
            val patientIds = therapistDoc.get("assignedPatient") as? List<String> ?: emptyList()
            
            if (patientIds.isEmpty()) {
                return emptyList()
            }
            
            // Fetch details for each patient
            val patients = mutableListOf<PatientInfo>()
            
            patientIds.forEach { patientId ->
                getPatientInfo(patientId)?.let { patients.add(it) }
            }
            
            return patients
        } catch (e: Exception) {
            Log.e(TAG, "Error getting assigned patients: ${e.message}", e)
            return emptyList()
        }
    }
    
    /**
     * Start a new chat with a patient (alias for getOrCreateChatThread)
     */
    suspend fun startNewChat(patientId: String): ChatThread? {
        return getOrCreateChatThread(patientId)
    }
    
    /**
     * Get all patients the therapist has chatted with
     */
    suspend fun getChatPatients(): List<PatientInfo> {
        val therapistId = getCurrentTherapistId() ?: return emptyList()
        
        try {
            // Query all chats for this therapist
            val chatDocs = db.collection(CHATS_COLLECTION)
                .whereEqualTo("therapistId", therapistId)
                .get()
                .await()
                
            if (chatDocs.isEmpty) {
                return emptyList()
            }
            
            // Extract patient IDs from chats
            val patientIds = chatDocs.documents.mapNotNull { doc ->
                doc.getString("patientId")
            }.distinct()
            
            // Fetch patient details
            val patients = mutableListOf<PatientInfo>()
            patientIds.forEach { patientId ->
                getPatientInfo(patientId)?.let { patients.add(it) }
            }
            
            return patients
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chat patients: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Send an exercise through chat
     */
    suspend fun sendExerciseMessage(chatId: String, patientId: String, exerciseId: String): Boolean {
        val therapistId = getCurrentTherapistId() ?: return false
        
        try {
            // Get exercise details
            val exerciseDoc = db.collection(USERS_COLLECTION)
                .document(patientId)
                .collection(EXERCISES_SUBCOLLECTION)
                .document(exerciseId)
                .get()
                .await()
                
            if (!exerciseDoc.exists()) return false
            
            val exerciseName = exerciseDoc.getString("name") ?: "Exercise"
            val exerciseMessage = "I've assigned you a new exercise: $exerciseName. Please check your exercises tab."
            
            // Send the message about the exercise
            return sendMessage(chatId, exerciseMessage, patientId)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending exercise message: ${e.message}", e)
            return false
        }
    }

    /**
     * Mark a chat thread as read for the therapist
     */
    private fun markChatAsRead(chatId: String, therapistId: String) {
        try {
            val updates = hashMapOf<String, Any>(
                "unreadCount.${therapistId}" to 0
            )
            
            db.collection(CHATS_COLLECTION)
                .document(chatId)
                .set(updates, SetOptions.merge())
        } catch (e: Exception) {
            Log.e(TAG, "Error marking chat as read: ${e.message}", e)
        }
    }

    /**
     * Mark new messages as read
     */
    private fun markNewMessagesAsRead(chatId: String, messages: List<ChatMessage>, therapistId: String) {
        try {
            val batch = db.batch()
            
            // Mark patient messages as read
            messages.forEach { message ->
                if (message.senderType == SenderType.PATIENT && !message.isRead) {
                    val messageRef = db.collection(CHATS_COLLECTION)
                        .document(chatId)
                        .collection(MESSAGES_SUBCOLLECTION)
                        .document(message.id)
                        
                    batch.update(messageRef, "isRead", true)
                }
            }
            
            batch.commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read: ${e.message}", e)
        }
    }

    /**
     * Map a Firestore document to a ChatThread object
     */
    private fun mapDocumentToChatThread(doc: DocumentSnapshot): ChatThread {
        val id = doc.id
        val therapistId = doc.getString("therapistId") ?: ""
        val patientId = doc.getString("patientId") ?: ""
        val lastActivity = (doc.get("lastActivity") as? Timestamp)?.toDate() ?: Date()
        
        // Get last message if available
        val lastMessageText = doc.getString("lastMessage")
        val lastMessage = if (lastMessageText != null) {
            ChatMessage(
                message = lastMessageText,
                timestamp = lastActivity
            )
        } else null
        
        // Get unread count for therapist
        val unreadCountMap = doc.get("unreadCount") as? Map<String, Long>
        val unreadCount = unreadCountMap?.get(therapistId)?.toInt() ?: 0
        
        return ChatThread(
            id = id,
            therapistId = therapistId,
            patientId = patientId,
            lastMessage = lastMessage,
            unreadCount = unreadCount,
            lastActivity = lastActivity
        )
    }
}
