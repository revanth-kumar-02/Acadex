package com.acadex.app.data.remote

import retrofit2.http.GET

data class AnnouncementDto(
    val id: String,
    val title: String,
    val content: String,
    val date: String
)

data class QuoteDto(
    val quote: String,
    val author: String
)

interface ApiService {
    @GET("api/announcements")
    suspend fun getAnnouncements(): List<AnnouncementDto>

    @GET("api/quote")
    suspend fun getRandomQuote(): QuoteDto
}
