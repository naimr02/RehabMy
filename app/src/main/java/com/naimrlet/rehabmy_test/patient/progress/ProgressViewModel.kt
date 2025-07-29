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
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ProgressData(
    val date: Date,
    val completionPercentage: Float,
    val totalExercises: Int,
    val completedExercises: Int
)

data class DateRangeInfo(
    val startDate: Date,
    val endDate: Date,
    val period: ProgressPeriod
)

enum class ProgressPeriod {
    WEEKLY, MONTHLY, YEARLY
}

@HiltViewModel
class ProgressViewModel @Inject constructor() : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Malaysian timezone (UTC+8)
    private val malaysianTimeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")

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

    private val _currentDateRange = MutableStateFlow<DateRangeInfo?>(null)
    val currentDateRange: StateFlow<DateRangeInfo?> = _currentDateRange

    // Navigation offsets for each period
    private val _weekOffset = MutableStateFlow(0)
    private val _monthOffset = MutableStateFlow(0)
    private val _yearOffset = MutableStateFlow(0)

    // Cached exercises to avoid re-fetching
    private var cachedExercises: List<Exercise> = emptyList()

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
        refreshCurrentPeriodData()
    }

    fun navigateToPrevious() {
        when (_selectedPeriod.value) {
            ProgressPeriod.WEEKLY -> {
                _weekOffset.value = _weekOffset.value - 1
                processWeeklyData(cachedExercises, _weekOffset.value)
            }
            ProgressPeriod.MONTHLY -> {
                _monthOffset.value = _monthOffset.value - 1
                processMonthlyData(cachedExercises, _monthOffset.value)
            }
            ProgressPeriod.YEARLY -> {
                _yearOffset.value = _yearOffset.value - 1
                processYearlyData(cachedExercises, _yearOffset.value)
            }
        }
        updateCurrentDateRange()
    }

    fun navigateToNext() {
        when (_selectedPeriod.value) {
            ProgressPeriod.WEEKLY -> {
                if (_weekOffset.value < 0) {
                    _weekOffset.value = _weekOffset.value + 1
                    processWeeklyData(cachedExercises, _weekOffset.value)
                }
            }
            ProgressPeriod.MONTHLY -> {
                if (_monthOffset.value < 0) {
                    _monthOffset.value = _monthOffset.value + 1
                    processMonthlyData(cachedExercises, _monthOffset.value)
                }
            }
            ProgressPeriod.YEARLY -> {
                if (_yearOffset.value < 0) {
                    _yearOffset.value = _yearOffset.value + 1
                    processYearlyData(cachedExercises, _yearOffset.value)
                }
            }
        }
        updateCurrentDateRange()
    }

    fun canNavigateNext(): Boolean {
        return when (_selectedPeriod.value) {
            ProgressPeriod.WEEKLY -> _weekOffset.value < 0
            ProgressPeriod.MONTHLY -> _monthOffset.value < 0
            ProgressPeriod.YEARLY -> _yearOffset.value < 0
        }
    }

    private fun refreshCurrentPeriodData() {
        when (_selectedPeriod.value) {
            ProgressPeriod.WEEKLY -> processWeeklyData(cachedExercises, _weekOffset.value)
            ProgressPeriod.MONTHLY -> processMonthlyData(cachedExercises, _monthOffset.value)
            ProgressPeriod.YEARLY -> processYearlyData(cachedExercises, _yearOffset.value)
        }
        updateCurrentDateRange()
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
        cachedExercises = exercises

        // Process all periods with current offsets
        processWeeklyData(exercises, _weekOffset.value)
        processMonthlyData(exercises, _monthOffset.value)
        processYearlyData(exercises, _yearOffset.value)

        updateCurrentDateRange()
    }

    private fun processWeeklyData(exercises: List<Exercise>, weekOffset: Int) {
        val calendar = Calendar.getInstance(malaysianTimeZone)

        // Get the start of the target week (Monday to Sunday)
        calendar.add(Calendar.WEEK_OF_YEAR, weekOffset)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val weeklyData = mutableListOf<ProgressData>()

        // Process each day of the week (Monday to Sunday)
        for (dayIndex in 0..6) {
            val targetDate = calendar.time

            // Set end of day
            val endCalendar = Calendar.getInstance(malaysianTimeZone)
            endCalendar.time = targetDate
            endCalendar.set(Calendar.HOUR_OF_DAY, 23)
            endCalendar.set(Calendar.MINUTE, 59)
            endCalendar.set(Calendar.SECOND, 59)
            endCalendar.set(Calendar.MILLISECOND, 999)
            val dayEnd = endCalendar.time

            // Find exercises due on this day
            val exercisesDueToday = exercises.filter { exercise ->
                exercise.dueDate?.toDate()?.let { dueDate ->
                    isSameDay(dueDate, targetDate)
                } ?: false
            }

            val completedToday = exercisesDueToday.count { exercise ->
                exercise.completed && (
                    exercise.completedDate?.toDate()?.let { completedDate ->
                        completedDate <= dayEnd
                    } ?: false
                )
            }

            val totalDueToday = exercisesDueToday.size
            val percentage = if (totalDueToday > 0) (completedToday.toFloat() / totalDueToday) * 100 else 0f

            weeklyData.add(ProgressData(targetDate, percentage, totalDueToday, completedToday))

            // Move to next day
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        _weeklyProgress.value = weeklyData
    }

    private fun processMonthlyData(exercises: List<Exercise>, monthOffset: Int) {
        val calendar = Calendar.getInstance(malaysianTimeZone)

        // Get the target month (1st to last day)
        calendar.add(Calendar.MONTH, monthOffset)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val monthlyData = mutableListOf<ProgressData>()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Process each day of the month (1st to last day)
        for (dayIndex in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, dayIndex)
            val targetDate = calendar.time

            // Set end of day
            val endCalendar = Calendar.getInstance(malaysianTimeZone)
            endCalendar.time = targetDate
            endCalendar.set(Calendar.HOUR_OF_DAY, 23)
            endCalendar.set(Calendar.MINUTE, 59)
            endCalendar.set(Calendar.SECOND, 59)
            endCalendar.set(Calendar.MILLISECOND, 999)
            val dayEnd = endCalendar.time

            // Find exercises due on this day
            val exercisesDueToday = exercises.filter { exercise ->
                exercise.dueDate?.toDate()?.let { dueDate ->
                    isSameDay(dueDate, targetDate)
                } ?: false
            }

            val completedToday = exercisesDueToday.count { exercise ->
                exercise.completed && (
                    exercise.completedDate?.toDate()?.let { completedDate ->
                        completedDate <= dayEnd
                    } ?: false
                )
            }

            val totalDueToday = exercisesDueToday.size
            val percentage = if (totalDueToday > 0) (completedToday.toFloat() / totalDueToday) * 100 else 0f

            monthlyData.add(ProgressData(targetDate, percentage, totalDueToday, completedToday))
        }

        _monthlyProgress.value = monthlyData
    }

    private fun processYearlyData(exercises: List<Exercise>, yearOffset: Int) {
        val calendar = Calendar.getInstance(malaysianTimeZone)

        // Get the target year (January 1st to December 31st)
        calendar.add(Calendar.YEAR, yearOffset)
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val yearlyData = mutableListOf<ProgressData>()

        // Process each month of the year (January to December)
        for (monthIndex in 0..11) {
            calendar.set(Calendar.MONTH, monthIndex)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = calendar.time

            // Get last day of month
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val monthEnd = calendar.time

            // Find exercises in this month
            val monthExercises = exercises.filter { exercise ->
                exercise.dueDate?.toDate()?.let { dueDate ->
                    dueDate >= monthStart && dueDate <= monthEnd
                } ?: false
            }

            val completed = monthExercises.count { exercise ->
                exercise.completed && (
                    exercise.completedDate?.toDate()?.let { completedDate ->
                        completedDate <= monthEnd
                    } ?: false
                )
            }

            val total = monthExercises.size
            val percentage = if (total > 0) (completed.toFloat() / total) * 100 else 0f

            yearlyData.add(ProgressData(monthStart, percentage, total, completed))
        }

        _yearlyProgress.value = yearlyData
    }

    private fun updateCurrentDateRange() {
        val calendar = Calendar.getInstance(malaysianTimeZone)

        when (_selectedPeriod.value) {
            ProgressPeriod.WEEKLY -> {
                // Get Monday to Sunday of the current week being viewed
                calendar.add(Calendar.WEEK_OF_YEAR, _weekOffset.value)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val weekStart = calendar.time

                calendar.add(Calendar.DAY_OF_YEAR, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val weekEnd = calendar.time

                _currentDateRange.value = DateRangeInfo(weekStart, weekEnd, ProgressPeriod.WEEKLY)
            }
            ProgressPeriod.MONTHLY -> {
                // Get 1st to last day of the current month being viewed
                calendar.add(Calendar.MONTH, _monthOffset.value)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.time

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val monthEnd = calendar.time

                _currentDateRange.value = DateRangeInfo(monthStart, monthEnd, ProgressPeriod.MONTHLY)
            }
            ProgressPeriod.YEARLY -> {
                // Get January 1st to December 31st of the current year being viewed
                calendar.add(Calendar.YEAR, _yearOffset.value)
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val yearStart = calendar.time

                calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val yearEnd = calendar.time

                _currentDateRange.value = DateRangeInfo(yearStart, yearEnd, ProgressPeriod.YEARLY)
            }
        }
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
