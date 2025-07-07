package com.naimrlet.rehabmy_test.therapist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class ExerciseViewModel(
    private val patientId: String? = null
) : ViewModel() {
    private val TAG = "ExerciseViewModel"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // State flows for UI state
    private val _exercises = MutableStateFlow<List<ExerciseInfo>>(emptyList())
    val exercises: StateFlow<List<ExerciseInfo>> = _exercises.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Create a CoroutineExceptionHandler that properly handles JobCancellationException
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        when (throwable) {
            is CancellationException -> {
                // Log but don't treat as an error since this is expected during lifecycle changes
                Log.d(TAG, "Coroutine was cancelled safely", throwable)
            }
            else -> {
                // For all other exceptions, update the error state
                Log.e(TAG, "Error in coroutine: ${throwable.message}", throwable)
                _error.value = throwable.message ?: "Unknown error occurred"
                _isLoading.value = false
            }
        }
    }

    init {
        // Load exercises automatically when ViewModel is created
        loadExercises()
    }

    /**
     * Load exercises from Firestore with proper cancellation handling
     */
    fun loadExercises() {
        // Determine which user's exercises to load
        val userId = patientId ?: auth.currentUser?.uid ?: return
        
        // Reset states
        _isLoading.value = true
        _error.value = null
        
        // Launch coroutine with exception handler
        viewModelScope.launch(exceptionHandler) {
            try {
                // Check if job is active before performing the operation
                if (!coroutineContext.isActive) return@launch
                
                val exercisesSnapshot = db.collection("users")
                    .document(userId)
                    .collection("exercises")
                    .get()
                    .await()
                
                // Check again if job is still active after the await call
                if (!coroutineContext.isActive) return@launch
                
                val exercisesList = exercisesSnapshot.documents.mapNotNull { doc ->
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
                        Log.e(TAG, "Error parsing exercise: ${e.message}", e)
                        null
                    }
                }
                
                _exercises.value = exercisesList
                _isLoading.value = false
            } catch (e: CancellationException) {
                // Let it propagate to the exception handler
                Log.e(TAG, "Error loading exercises", e)
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error loading exercises: ${e.message}", e)
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    /**
     * Update exercise completion status
     */
    fun updateExercise(exerciseId: String, completed: Boolean, painLevel: Int = 0, comments: String = "") {
        val userId = patientId ?: auth.currentUser?.uid ?: return
        
        viewModelScope.launch(exceptionHandler) {
            try {
                // Check if job is active before performing the operation
                if (!coroutineContext.isActive) return@launch
                
                val updates = hashMapOf<String, Any>(
                    "completed" to completed,
                    "painLevel" to painLevel,
                    "comments" to comments,
                    "lastUpdated" to Date()
                )
                
                db.collection("users")
                    .document(userId)
                    .collection("exercises")
                    .document(exerciseId)
                    .update(updates)
                    .await()
                
                // Reload exercises to reflect changes
                loadExercises()
            } catch (e: CancellationException) {
                // Let it propagate to the exception handler
                Log.e(TAG, "Update cancelled", e)
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error updating exercise: ${e.message}", e)
                _error.value = e.message
            }
        }
    }

    /**
     * Clear any error message
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Factory for creating the ExerciseViewModel with dependencies
     */
    class Factory(private val patientId: String? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExerciseViewModel::class.java)) {
                return ExerciseViewModel(patientId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
