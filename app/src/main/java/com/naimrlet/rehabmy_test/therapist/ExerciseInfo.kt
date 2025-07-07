package com.naimrlet.rehabmy_test.therapist

import java.util.Date

data class ExerciseInfo(
    val id: String,
    val name: String,
    val description: String,
    val duration: Int, // in minutes
    val frequency: String,
    val completed: Boolean = false, // Changed from float to boolean
    val painLevel: Int = 0,
    val comments: String = "",
    val dueDate: Date? = null
)
