package com.naimrlet.rehabmy_test.patient.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.naimrlet.rehabmy_test.model.BodyPart
import com.naimrlet.rehabmy_test.model.LibraryExercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LibraryViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _exercises = MutableStateFlow<List<LibraryExercise>>(emptyList())
    val exercises: StateFlow<List<LibraryExercise>> = _exercises
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    private val _selectedBodyPart = MutableStateFlow<BodyPart?>(null)
    val selectedBodyPart: StateFlow<BodyPart?> = _selectedBodyPart

    // Store listener registrations for cleanup
    private val listeners = mutableListOf<ListenerRegistration>()
    // Store exercises by body part to manage updates efficiently
    private val exercisesByBodyPart = mutableMapOf<BodyPart, MutableList<LibraryExercise>>()

    init {
        setupExerciseListeners()
    }

    private fun setupExerciseListeners() {
        viewModelScope.launch {
            try {
                _loading.value = true

                // Initialize exercise lists for each body part
                BodyPart.entries.forEach { exercisesByBodyPart[it] = mutableListOf() }

                // Set up a listener for each body part
                for (bodyPart in BodyPart.entries) {
                    val bodyPartName = bodyPart.name.lowercase()
                    Log.d("LibraryViewModel", "Setting up listener for body part: $bodyPartName at path: library/exercises/$bodyPartName")

                    val listener = db.collection("library")
                        .document("exercises")
                        .collection(bodyPartName)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Log.e("LibraryViewModel", "Error listening for $bodyPartName exercises", error)
                                return@addSnapshotListener
                            }

                            if (snapshot != null) {
                                Log.d("LibraryViewModel", "Received data for $bodyPartName: ${snapshot.documents.size} documents")

                                val exercisesForBodyPart = snapshot.documents.mapNotNull { doc ->
                                    try {
                                        // Each document represents an exercise at path /library/exercises/[bodyPart]/[DocID]
                                        val exercise = doc.toObject(LibraryExercise::class.java)?.copy(
                                            id = doc.id,
                                            bodyPart = bodyPart
                                        )
                                        Log.d("LibraryViewModel", "Parsed exercise: ${exercise?.name} with ID: ${doc.id} for $bodyPartName")
                                        exercise
                                    } catch (e: Exception) {
                                        Log.e("LibraryViewModel", "Error converting document ${doc.id} for $bodyPartName", e)
                                        null
                                    }
                                }

                                // Update the exercises for this body part
                                exercisesByBodyPart[bodyPart] = exercisesForBodyPart.toMutableList()

                                // Update the overall exercises list with all body parts
                                updateExercisesList()

                                Log.d("LibraryViewModel", "Updated ${exercisesForBodyPart.size} exercises for $bodyPartName")
                            } else {
                                Log.d("LibraryViewModel", "Snapshot is null for $bodyPartName")
                            }
                        }

                    // Store the listener for later cleanup
                    listeners.add(listener)
                }

                _loading.value = false
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error setting up library exercise listeners", e)
                _loading.value = false
            }
        }
    }

    private fun updateExercisesList() {
        val allExercises = mutableListOf<LibraryExercise>()
        exercisesByBodyPart.forEach { (bodyPart, exercises) ->
            Log.d("LibraryViewModel", "Adding ${exercises.size} exercises for ${bodyPart.name}")
            allExercises.addAll(exercises)
        }
        _exercises.value = allExercises
        Log.d("LibraryViewModel", "Updated total exercises count: ${allExercises.size}")
    }

    fun selectBodyPart(bodyPart: BodyPart) {
        _selectedBodyPart.value = bodyPart
        Log.d("LibraryViewModel", "Selected body part: ${bodyPart.name}")

        // Log the count of exercises for this body part for debugging
        val exercisesForSelectedBodyPart = getExercisesByBodyPart(bodyPart)
        Log.d("LibraryViewModel", "Found ${exercisesForSelectedBodyPart.size} exercises for ${bodyPart.name}")
    }

    fun clearSelection() {
        _selectedBodyPart.value = null
    }

    fun getExercisesByBodyPart(bodyPart: BodyPart): List<LibraryExercise> {
        val exercises = _exercises.value.filter { it.bodyPart == bodyPart }
        Log.d("LibraryViewModel", "Filtering exercises for ${bodyPart.name}: ${exercises.size} found")
        return exercises
    }

    override fun onCleared() {
        // Remove all listeners when ViewModel is cleared to prevent memory leaks
        listeners.forEach { it.remove() }
        listeners.clear()
        super.onCleared()
    }
}
