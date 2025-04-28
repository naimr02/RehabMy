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

    // Add debug tag
    companion object {
        private const val TAG = "ExerciseViewModel"
    }

    fun loadExercises() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Attempting to load exercises...")

                val user = auth.currentUser
                if (user == null) {
                    Log.e(TAG, "No authenticated user found!")
                    _loading.value = false
                    return@launch
                }

                val userId = user.uid
                Log.d(TAG, "Current user ID: $userId")

                // Modify the snapshot listener to include metadata changes
                db.collection("users")
                    .document(userId)
                    .collection("exercises")
                    .snapshots(MetadataChanges.INCLUDE) // Add this parameter
                    .collect { snapshot ->
                        Log.d(TAG, "Received snapshot with ${snapshot.size()} documents")
                        Log.d(TAG, "From cache: ${snapshot.metadata.isFromCache}")
                        Log.d(TAG, "Has pending writes: ${snapshot.metadata.hasPendingWrites()}")

                        // Process all snapshots, don't skip ones with pending writes
                        val exerciseList = snapshot.documents.mapNotNull { doc ->
                            try {
                                Log.d(TAG, "Processing document ${doc.id}")
                                val exercise = doc.toObject(Exercise::class.java)
                                exercise?.copy(id = doc.id).also {
                                    Log.d(TAG, "Mapped exercise: ${it?.name}, completed: ${it?.completed}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error converting document ${doc.id}", e)
                                null
                            }
                        }

                        Log.d(TAG, "Final exercise list size: ${exerciseList.size}")
                        _exercises.value = exerciseList
                        _loading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadExercises", e)
                _loading.value = false
            }
        }
    }



    fun updateExerciseStatus(exercise: Exercise, completed: Boolean) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: run {
                    Log.e(TAG, "No user authenticated during update")
                    return@launch
                }

                Log.d(TAG, "Updating exercise ${exercise.id} completed status to $completed")

                db.collection("users")
                    .document(userId)
                    .collection("exercises")
                    .document(exercise.id)
                    .update("completed", completed)
                    .addOnSuccessListener {
                        Log.d(TAG, "Update successful for exercise ${exercise.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Update failed for exercise ${exercise.id}", e)
                    }
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error in updateExerciseStatus", e)
            }
        }
    }
}
