package com.acadex.app.domain.model

enum class AssignmentPriority {
    LOW, MEDIUM, HIGH
}

enum class AssignmentStatus {
    PENDING, IN_PROGRESS, COMPLETED
}

data class Assignment(
    val id: String = "",
    val title: String,
    val description: String,
    val subject: String,
    val dueDate: Long,
    val priority: AssignmentPriority = AssignmentPriority.MEDIUM,
    val status: AssignmentStatus = AssignmentStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)
