package com.acadex.app.domain.model

enum class TaskType {
    STUDY, TASK, DEADLINE
}

data class PlannerTask(
    val id: String = "",
    val title: String,
    val description: String = "",
    val date: Long, // Day in epoch millis
    val startTime: String = "",
    val endTime: String = "",
    val type: TaskType = TaskType.TASK,
    val isCompleted: Boolean = false
)
