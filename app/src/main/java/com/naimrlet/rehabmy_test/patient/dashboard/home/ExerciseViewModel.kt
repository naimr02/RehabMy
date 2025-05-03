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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ExerciseViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    companion object {
        private const val TAG = "ExerciseViewModel"
        private const val USERS_COLLECTION = "users"
        private const val EXERCISES_COLLECTION = "exercises"
        private const val COMPLETED_FIELD = "completed"
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

    fun updateExerciseStatus(exercise: Exercise, completed: Boolean) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId() ?: return@launch

                db.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(EXERCISES_COLLECTION)
                    .document(exercise.id)
                    .update(COMPLETED_FIELD, completed)
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating exercise status", e)
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
