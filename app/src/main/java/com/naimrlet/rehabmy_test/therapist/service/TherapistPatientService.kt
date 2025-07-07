package com.naimrlet.rehabmy_test.therapist.service

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.naimrlet.rehabmy_test.model.Exercise
import com.naimrlet.rehabmy_test.therapist.PatientInfo
import com.naimrlet.rehabmy_test.therapist.ExerciseInfo
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TherapistPatientService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "TherapistPatientService"
        private const val USERS_COLLECTION = "users"
        private const val EXERCISES_COLLECTION = "exercises"
    }

    private fun getCurrentTherapistId(): String? = auth.currentUser?.uid

    /**
     * Get all patients assigned to the current therapist
     * Reads from users/{therapistId}/assignedPatient array
     */
    suspend fun getAssignedPatients(): Result<List<PatientInfo>> {
        return try {
            val therapistId = getCurrentTherapistId()
                ?: return Result.failure(Exception("Therapist not authenticated"))

            // Get therapist document to read assignedPatient array
            val therapistDoc = firestore.collection(USERS_COLLECTION)
                .document(therapistId)
                .get()
                .await()

            if (!therapistDoc.exists()) {
                return Result.failure(Exception("Therapist document not found"))
            }

            // Get assigned patient IDs from the array
            val assignedPatientIds = therapistDoc.get("assignedPatient") as? List<String> ?: emptyList()

            if (assignedPatientIds.isEmpty()) {
                return Result.success(emptyList())
            }

            // Fetch patient details for each assigned patient
            val patients = mutableListOf<PatientInfo>()

            for (patientId in assignedPatientIds) {
                try {
                    val patientInfo = getPatientDetails(patientId)
                    if (patientInfo != null) {
                        patients.add(patientInfo)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch patient $patientId: ${e.message}")
                    // Continue with other patients even if one fails
                }
            }

            Result.success(patients)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting assigned patients", e)
            Result.failure(e)
        }
    }

    /**
     * Get detailed information for a specific patient
     */
    suspend fun getPatientDetails(patientId: String): PatientInfo? {
        return try {
            // Get patient basic info
            val patientDoc = firestore.collection(USERS_COLLECTION)
                .document(patientId)
                .get()
                .await()

            if (!patientDoc.exists()) {
                Log.w(TAG, "Patient document not found: $patientId")
                return null
            }

            // Get patient's exercises
            val exercisesSnapshot = firestore.collection(USERS_COLLECTION)
                .document(patientId)
                .collection(EXERCISES_COLLECTION)
                .get()
                .await()

            // Convert exercises to ExerciseInfo objects
            val exercises = exercisesSnapshot.documents.mapNotNull { exerciseDoc ->
                try {
                    ExerciseInfo(
                        id = exerciseDoc.id,
                        name = exerciseDoc.getString("name") ?: "Unknown Exercise",
                        description = exerciseDoc.getString("description") ?: "",
                        duration = exerciseDoc.getLong("duration")?.toInt() ?: 10, // Read from Firestore
                        frequency = exerciseDoc.getString("frequency") ?: "Daily", // Read from Firestore
                        painLevel = exerciseDoc.getLong("painLevel")?.toInt() ?: 0,
                        comments = exerciseDoc.getString("comments") ?: "",
                        dueDate = exerciseDoc.getDate("dueDate"),
                        completed = exerciseDoc.getBoolean("completed") ?: false
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing exercise for patient $patientId: ${e.message}")
                    null
                }
            }

            // Calculate completion statistics
            val completedExercises = exercises.count { it.completed }

            PatientInfo(
                id = patientId,
                name = patientDoc.getString("name") ?: "Unknown Patient",
                age = patientDoc.getLong("age")?.toInt() ?: 0,
                condition = patientDoc.getString("condition") ?: "Not specified",
                completedExercises = completedExercises,
                totalExercises = exercises.size,
                assignedExercises = exercises
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting patient details for $patientId", e)
            null
        }
    }

    /**
     * Get exercise completion statistics for a patient
     */
    suspend fun getPatientProgress(patientId: String): Result<Map<String, Any>> {
        return try {
            val exercises = firestore.collection(USERS_COLLECTION)
                .document(patientId)
                .collection(EXERCISES_COLLECTION)
                .get()
                .await()

            val totalExercises = exercises.size()
            val completedExercises = exercises.documents.count { doc ->
                doc.getBoolean("completed") ?: false
            }

            val completionRate = if (totalExercises > 0) {
                (completedExercises.toFloat() / totalExercises) * 100
            } else 0f

            val progressData = mapOf(
                "totalExercises" to totalExercises,
                "completedExercises" to completedExercises,
                "completionRate" to completionRate,
                "patientId" to patientId
            )

            Result.success(progressData)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting patient progress for $patientId", e)
            Result.failure(e)
        }
    }

    /**
     * Add a patient to therapist's assigned list
     */
    suspend fun assignPatientToTherapist(patientId: String): Result<Boolean> {
        return try {
            val therapistId = getCurrentTherapistId()
                ?: return Result.failure(Exception("Therapist not authenticated"))

            // Add patient to therapist's assignedPatient array
            firestore.collection(USERS_COLLECTION)
                .document(therapistId)
                .update("assignedPatient", com.google.firebase.firestore.FieldValue.arrayUnion(patientId))
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error assigning patient $patientId to therapist", e)
            Result.failure(e)
        }
    }

    /**
     * Remove a patient from therapist's assigned list
     */
    suspend fun unassignPatientFromTherapist(patientId: String): Result<Boolean> {
        return try {
            val therapistId = getCurrentTherapistId()
                ?: return Result.failure(Exception("Therapist not authenticated"))

            // Remove patient from therapist's assignedPatient array
            firestore.collection(USERS_COLLECTION)
                .document(therapistId)
                .update("assignedPatient", com.google.firebase.firestore.FieldValue.arrayRemove(patientId))
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error unassigning patient $patientId from therapist", e)
            Result.failure(e)
        }
    }
}
