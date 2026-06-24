package com.acadex.app.domain.model

data class User(
    val uid: String,
    val name: String,
    val email: String,
    val registerNumber: String = "",
    val department: String = "",
    val semester: String = "",
    val profilePicUrl: String = ""
)
