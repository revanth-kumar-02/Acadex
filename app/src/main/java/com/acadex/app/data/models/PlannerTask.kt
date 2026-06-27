package com.acadex.app.data.models

enum class TaskType {
    STUDY, TASK, DEADLINE
}

data class PlannerTask(
    val id: String = "",
    val title: String,
    val description: String = "",
    val dueDate: Long, // Day in epoch millis
    val startTime: String = "",
    val endTime: String = "",
    val type: TaskType = TaskType.TASK,
    val completed: Boolean = false
)
