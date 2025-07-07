package com.naimrlet.rehabmy_test.patient.progress

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.naimrlet.rehabmy_test.model.Exercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class ProgressData(
    val date: Date,
    val completionPercentage: Float,
    val totalExercises: Int,
    val completedExercises: Int
)

enum class ProgressPeriod {
    WEEKLY, MONTHLY, YEARLY
}

@HiltViewModel
class ProgressViewModel @Inject constructor() : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _weeklyProgress = MutableStateFlow<List<ProgressData>>(emptyList())
    val weeklyProgress: StateFlow<List<ProgressData>> = _weeklyProgress

    private val _monthlyProgress = MutableStateFlow<List<ProgressData>>(emptyList())
    val monthlyProgress: StateFlow<List<ProgressData>> = _monthlyProgress

    private val _yearlyProgress = MutableStateFlow<List<ProgressData>>(emptyList())
    val yearlyProgress: StateFlow<List<ProgressData>> = _yearlyProgress

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _selectedPeriod = MutableStateFlow(ProgressPeriod.WEEKLY)
    val selectedPeriod: StateFlow<ProgressPeriod> = _selectedPeriod

    companion object {
        private const val TAG = "ProgressViewModel"
        private const val USERS_COLLECTION = "users"
        private const val EXERCISES_COLLECTION = "exercises"
    }

    init {
        loadProgressData()
    }

    fun setSelectedPeriod(period: ProgressPeriod) {
        _selectedPeriod.value = period
    }

    private fun loadProgressData() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId() ?: return@launch

                db.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(EXERCISES_COLLECTION)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val exercises = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Exercise::class.java)?.copy(id = doc.id)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error converting document ${doc.id}", e)
                                null
                            }
                        }

                        processProgressData(exercises)
                        _loading.value = false
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error loading exercises", e)
                        _loading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadProgressData", e)
                _loading.value = false
            }
        }
    }

    private fun processProgressData(exercises: List<Exercise>) {
        val calendar = Calendar.getInstance()
        val today = calendar.time

        // Process weekly data (last 7 days)
        val weeklyData = mutableListOf<ProgressData>()
        for (i in 6 downTo 0) {
            calendar.time = today
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = calendar.time

            val dayExercises = exercises.filter { exercise ->
                exercise.dueDate?.toDate()?.let { dueDate ->
                    isSameDay(dueDate, date)
                } ?: false
            }

            val completed = dayExercises.count { it.completed }
            val total = dayExercises.size
            val percentage = if (total > 0) (completed.toFloat() / total) * 100 else 0f

            weeklyData.add(ProgressData(date, percentage, total, completed))
        }
        _weeklyProgress.value = weeklyData

        // Process monthly data (last 4 weeks)
        val monthlyData = mutableListOf<ProgressData>()
        for (i in 3 downTo 0) {
            calendar.time = today
            calendar.add(Calendar.WEEK_OF_YEAR, -i)
            val weekStart = calendar.time

            calendar.add(Calendar.DAY_OF_YEAR, 6)
            val weekEnd = calendar.time

            val weekExercises = exercises.filter { exercise ->
                exercise.dueDate?.toDate()?.let { dueDate ->
                    dueDate.after(weekStart) && dueDate.before(weekEnd) ||
                    isSameDay(dueDate, weekStart) || isSameDay(dueDate, weekEnd)
                } ?: false
            }

            val completed = weekExercises.count { it.completed }
            val total = weekExercises.size
            val percentage = if (total > 0) (completed.toFloat() / total) * 100 else 0f

            monthlyData.add(ProgressData(weekStart, percentage, total, completed))
        }
        _monthlyProgress.value = monthlyData

        // Process yearly data (last 12 months)
        val yearlyData = mutableListOf<ProgressData>()
        for (i in 11 downTo 0) {
            calendar.time = today
            calendar.add(Calendar.MONTH, -i)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = calendar.time

            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val monthEnd = calendar.time

            val monthExercises = exercises.filter { exercise ->
                exercise.dueDate?.toDate()?.let { dueDate ->
                    dueDate.after(monthStart) && dueDate.before(monthEnd) ||
                    isSameDay(dueDate, monthStart) || isSameDay(dueDate, monthEnd)
                } ?: false
            }

            val completed = monthExercises.count { it.completed }
            val total = monthExercises.size
            val percentage = if (total > 0) (completed.toFloat() / total) * 100 else 0f

            yearlyData.add(ProgressData(monthStart, percentage, total, completed))
        }
        _yearlyProgress.value = yearlyData
    }

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid.also {
            if (it == null) {
                Log.e(TAG, "No authenticated user found")
            }
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
