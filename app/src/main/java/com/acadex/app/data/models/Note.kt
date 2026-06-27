package com.acadex.app.data.models

data class Note(
    val id: String = "",
    val title: String,
    val category: String, // "Notes", "PDF", "Document", "Study Resource"
    val subject: String,
    val fileUrl: String,
    val fileName: String,
    val broadcastTarget: String,
    val uploadedBy: String,
    val uploadedByName: String,
    val createdAt: Long = System.currentTimeMillis()
)
