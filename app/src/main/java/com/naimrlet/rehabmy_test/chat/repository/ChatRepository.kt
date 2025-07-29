package com.naimrlet.rehabmy_test.chat.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.naimrlet.rehabmy_test.chat.model.ChatMessage
import com.naimrlet.rehabmy_test.chat.model.ChatUser
import com.naimrlet.rehabmy_test.chat.model.ConversationPreview
import com.naimrlet.rehabmy_test.chat.model.UserType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "ChatRepository"
        private const val USERS_COLLECTION = "users"
        private const val CONVERSATIONS_COLLECTION = "conversations"
        private const val MESSAGES_COLLECTION = "messages"
    }

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun getCurrentUser(): Result<ChatUser> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            val userDoc = firestore.collection(USERS_COLLECTION).document(userId).get().await()

            if (!userDoc.exists()) {
                return Result.failure(Exception("User document not found"))
            }

            val user = ChatUser(
                id = userId,
                name = userDoc.getString("name") ?: "",
                email = userDoc.getString("email") ?: "",
                profileImageUrl = userDoc.getString("profileImageUrl") ?: "",
                isTherapist = userDoc.getBoolean("isTherapist") ?: false
            )

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user", e)
            Result.failure(e)
        }
    }

    suspend fun findChatPartner(): Result<ChatUser?> {
        return try {
            val currentUser = getCurrentUser().getOrThrow()
            val userId = currentUser.id

            val chatPartner = if (currentUser.isTherapist) {
                // Therapist: find patients assigned to them
                val therapistDoc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
                val assignedPatients = therapistDoc.get("assignedPatient") as? List<String> ?: emptyList()

                if (assignedPatients.isNotEmpty()) {
                    val patientDoc = firestore.collection(USERS_COLLECTION)
                        .document(assignedPatients.first())
                        .get().await()

                    if (patientDoc.exists()) {
                        ChatUser(
                            id = assignedPatients.first(),
                            name = patientDoc.getString("name") ?: "",
                            email = patientDoc.getString("email") ?: "",
                            profileImageUrl = patientDoc.getString("profileImageUrl") ?: "",
                            isTherapist = false
                        )
                    } else null
                } else null
            } else {
                // Patient: find their assigned therapist
                val therapistQuery = firestore.collection(USERS_COLLECTION)
                    .whereEqualTo("isTherapist", true)
                    .whereArrayContains("assignedPatient", userId)
                    .limit(1)
                    .get().await()

                if (!therapistQuery.isEmpty) {
                    val therapistDoc = therapistQuery.documents.first()
                    ChatUser(
                        id = therapistDoc.id,
                        name = therapistDoc.getString("name") ?: "",
                        email = therapistDoc.getString("email") ?: "",
                        profileImageUrl = therapistDoc.getString("profileImageUrl") ?: "",
                        isTherapist = true
                    )
                } else null
            }

            Result.success(chatPartner)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding chat partner", e)
            Result.failure(e)
        }
    }

    fun getMessagesFlow(otherUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            close(Exception("User not authenticated"))
            return@callbackFlow
        }

        // Determine the patient ID correctly based on current user type
        val conversationId = try {
            val currentUser = getCurrentUser().getOrThrow()
            if (currentUser.isTherapist) {
                // Therapist viewing chat: otherUserId is the patient ID
                otherUserId
            } else {
                // Patient viewing chat: currentUserId is the patient ID
                currentUserId
            }
        } catch (e: Exception) {
            close(Exception("Failed to determine conversation ID: ${e.message}"))
            return@callbackFlow
        }

        val messagesRef = firestore.collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_COLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val listener = messagesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val messages = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting message", e)
                    null
                }
            } ?: emptyList()

            trySend(messages)
        }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(
        recipientId: String,
        content: String,
        senderName: String,
        senderType: UserType
    ): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))

            val message = ChatMessage(
                senderId = currentUserId,
                senderName = senderName,
                senderType = senderType,
                content = content
            )

            // Always use patientId as conversation document ID, regardless of who is sending
            // For therapist: recipientId is patientId
            // For patient: currentUserId is the patientId (since patient is always the "owner" of the conversation)
            val conversationId = when (senderType) {
                UserType.THERAPIST -> recipientId // recipientId is the patientId
                UserType.PATIENT -> currentUserId // currentUserId is the patientId
            }

            // Store message in: conversations/{patientId}/messages
            firestore.collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .collection(MESSAGES_COLLECTION)
                .add(message)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(otherUserId: String): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))

            // Determine the patient ID correctly based on current user type
            val conversationId = try {
                val currentUser = getCurrentUser().getOrThrow()
                if (currentUser.isTherapist) {
                    // Therapist viewing chat: otherUserId is the patient ID
                    otherUserId
                } else {
                    // Patient viewing chat: currentUserId is the patient ID
                    currentUserId
                }
            } catch (e: Exception) {
                return Result.failure(Exception("Failed to determine conversation ID: ${e.message}"))
            }

            val messagesQuery = firestore.collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .collection(MESSAGES_COLLECTION)
                .whereEqualTo("senderId", otherUserId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = firestore.batch()
            messagesQuery.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read", e)
            Result.failure(e)
        }
    }

    /**
     * Get a specific user by their ID
     */
    suspend fun getUserById(userId: String): ChatUser? {
        return try {
            val userDoc = firestore.collection(USERS_COLLECTION).document(userId).get().await()

            if (!userDoc.exists()) {
                return null
            }

            ChatUser(
                id = userId,
                name = userDoc.getString("name") ?: "",
                email = userDoc.getString("email") ?: "",
                profileImageUrl = userDoc.getString("profileImageUrl") ?: "",
                isTherapist = userDoc.getBoolean("isTherapist") ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by ID: $userId", e)
            null
        }
    }
}
