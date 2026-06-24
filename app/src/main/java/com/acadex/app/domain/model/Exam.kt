package com.acadex.app.domain.model

data class Exam(
    val id: String = "",
    val subject: String,
    val examName: String,
    val dateTime: Long, // Epoch millis
    val room: String = "",
    val syllabus: String = "",
    val maxMarks: Int = 100,
    val targetMarks: Int = 90,
    val revisionProgress: Float = 0.0f // 0.0 to 1.0
)
