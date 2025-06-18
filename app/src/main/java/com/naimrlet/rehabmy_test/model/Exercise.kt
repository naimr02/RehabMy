package com.naimrlet.rehabmy_test.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Exercise(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val repetitions: Int = 0,
    val completed: Boolean = false,
    
    // New fields for patient feedback
    val painLevel: Int = 0,
    val comments: String = "",

    @PropertyName("assignedDate")
    val assignedDate: Timestamp? = null,

    @PropertyName("dueDate")
    val dueDate: Timestamp? = null,
    
    @PropertyName("completedDate")
    val completedDate: Timestamp? = null
)
