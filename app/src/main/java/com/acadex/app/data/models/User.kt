package com.acadex.app.data.models

data class User(
    val uid: String,
    val name: String,
    val email: String,
    val registerNumber: String = "",
    val department: String = "",
    val semester: String = "",
    val profilePicUrl: String = ""
)
