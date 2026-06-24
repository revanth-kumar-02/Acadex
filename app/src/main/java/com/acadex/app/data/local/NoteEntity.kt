package com.acadex.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val subject: String,
    val isFavorite: Boolean,
    val pdfUrl: String?,
    val pdfName: String?,
    val createdAt: Long,
    val updatedAt: Long
)
