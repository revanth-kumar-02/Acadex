package com.acadex.app.domain.model

data class Resource(
    val id: String = "",
    val title: String,
    val category: String, // "Previous Paper", "Syllabus", "Book", "Notes"
    val department: String,
    val semester: String,
    val subject: String,
    val fileUrl: String,
    val fileName: String,
    val downloadsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
