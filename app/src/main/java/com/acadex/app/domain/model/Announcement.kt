package com.acadex.app.domain.model

data class Announcement(
    val id: String = "",
    val userId: String,
    val title: String,
    val content: String,
    val broadcastTarget: String,
    val authorName: String,
    val createdAt: Long = System.currentTimeMillis()
)
