package com.naimrlet.rehabmy_test.patient.dashboard.home

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.naimrlet.rehabmy_test.model.Exercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ExerciseViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    companion object {
        private const val TAG = "ExerciseViewModel"
        private const val USERS_COLLECTION = "users"
        private const val EXERCISES_COLLECTION = "exercises"
        private const val COMPLETED_FIELD = "completed"
        private const val STORAGE_BASE_PATH = "patients/users"
    }

    fun loadExercises() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId() ?: return@launch

                db.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(EXERCISES_COLLECTION)
                    .snapshots(MetadataChanges.INCLUDE)
                    .collect { snapshot ->
                        val exerciseList = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Exercise::class.java)?.copy(id = doc.id)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error converting document ${doc.id}", e)
                                null
                            }
                        }
                        _exercises.value = exerciseList
                        _loading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading exercises", e)
                _loading.value = false
            }
        }
    }

    fun uploadVideoAndCompleteExercise(exerciseId: String, videoUri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _isUploading.value = true
                _uploadProgress.value = 0f

                val userId = getCurrentUserId() ?: run {
                    onComplete(false)
                    return@launch
                }

                val storageRef = storage.reference
                    .child("$STORAGE_BASE_PATH/$userId/exercises/${exerciseId}_${System.currentTimeMillis()}.mp4")

                val uploadTask = storageRef.putFile(videoUri)

                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    _uploadProgress.value = progress.toFloat() / 100f
                }

                uploadTask.await()

                // Get the download URL
                val downloadUrl = storageRef.downloadUrl.await().toString()

                // Update the exercise with the completed status and video URL
                db.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(EXERCISES_COLLECTION)
                    .document(exerciseId)
                    .update(
                        mapOf(
                            COMPLETED_FIELD to true,
                            "videoUrl" to downloadUrl
                        )
                    ).await()

                _isUploading.value = false
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading video", e)
                _isUploading.value = false
                onComplete(false)
            }
        }
    }

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid.also {
            if (it == null) {
                Log.e(TAG, "No authenticated user found")
                _loading.value = false
            }
        }
    }
}
