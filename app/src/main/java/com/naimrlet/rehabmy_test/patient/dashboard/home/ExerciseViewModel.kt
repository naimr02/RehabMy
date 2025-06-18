package com.naimrlet.rehabmy_test.patient.dashboard.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import com.naimrlet.rehabmy_test.model.Exercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class ExerciseViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    // Add selected date state
    private val _selectedDate = MutableStateFlow(Calendar.getInstance().time)
    val selectedDate: StateFlow<Date> = _selectedDate

    // Add filtered exercises by selected date
    val exercisesForSelectedDate = _selectedDate.map { date ->
        _exercises.value.filter { exercise ->
            exercise.dueDate?.toDate()?.let { dueDate ->
                isSameDay(dueDate, date)
            } ?: false
        }
    }

    companion object {
        private const val TAG = "ExerciseViewModel"
        private const val USERS_COLLECTION = "users"
        private const val EXERCISES_COLLECTION = "exercises"
        private const val COMPLETED_FIELD = "completed"
        private const val PAIN_LEVEL_FIELD = "painLevel"
        private const val COMMENTS_FIELD = "comments"
        private const val COMPLETED_DATE_FIELD = "completedDate"
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

    // Update the selected date
    fun setSelectedDate(date: Date) {
        _selectedDate.value = date
    }

    fun completeExerciseWithFeedback(
        exerciseId: String,
        painLevel: Int,
        comments: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isUploading.value = true

                val userId = getCurrentUserId() ?: run {
                    onComplete(false)
                    return@launch
                }

                // Update the exercise with the completed status, pain level, and comments
                db.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(EXERCISES_COLLECTION)
                    .document(exerciseId)
                    .update(
                        mapOf(
                            COMPLETED_FIELD to true,
                            PAIN_LEVEL_FIELD to painLevel,
                            COMMENTS_FIELD to comments,
                            COMPLETED_DATE_FIELD to com.google.firebase.Timestamp.now()
                        )
                    ).await()

                _isUploading.value = false
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error completing exercise", e)
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

    // Helper function to check if two dates are the same day
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
