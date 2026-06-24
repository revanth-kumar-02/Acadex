package com.acadex.app.data.mapper

import com.acadex.app.data.local.NoteEntity
import com.acadex.app.domain.model.Note

fun NoteEntity.toDomain(): Note {
    return Note(
        id = id,
        title = title,
        content = content,
        subject = subject,
        isFavorite = isFavorite,
        pdfUrl = pdfUrl,
        pdfName = pdfName,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        title = title,
        content = content,
        subject = subject,
        isFavorite = isFavorite,
        pdfUrl = pdfUrl,
        pdfName = pdfName,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
