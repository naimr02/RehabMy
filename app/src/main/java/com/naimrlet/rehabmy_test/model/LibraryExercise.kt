package com.naimrlet.rehabmy_test.model

data class LibraryExercise(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val bodyPart: BodyPart = BodyPart.HIP,
    val instructions: List<String> = emptyList(),
    val videoUrl: String = "",
    val imageUrl: String = ""
)

enum class BodyPart {
    HIP, KNEE, ANKLE, FOOT
}