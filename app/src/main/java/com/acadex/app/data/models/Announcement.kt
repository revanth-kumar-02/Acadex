package com.acadex.app.data.models

data class Announcement(
    val id: String = "",
    val userId: String,
    val title: String,
    val content: String,
    val broadcastTarget: String,
    val authorName: String,
    val createdAt: Long = System.currentTimeMillis()
)
