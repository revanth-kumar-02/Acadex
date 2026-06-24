package com.acadex.app.domain.model

data class Note(
    val id: String = "",
    val title: String,
    val content: String,
    val subject: String,
    val isFavorite: Boolean = false,
    val pdfUrl: String? = null,
    val pdfName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
