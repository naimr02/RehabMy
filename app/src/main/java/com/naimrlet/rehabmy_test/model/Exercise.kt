package com.naimrlet.rehabmy_test.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Exercise(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val repetitions: Int = 0,
    val completed: Boolean = false,

    @PropertyName("assignedDate")
    val assignedDate: Timestamp? = null,

    @PropertyName("dueDate")
    val dueDate: Timestamp? = null
)
