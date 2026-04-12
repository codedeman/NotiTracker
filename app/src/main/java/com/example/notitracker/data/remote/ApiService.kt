package com.example.notitracker.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("notifications")
    suspend fun getNotifications(): Response<List<NotificationDto>>

    @POST("ai/summarize")
    suspend fun getSummary(@Body request: SummaryRequest): Response<SummaryResponse>
}

data class NotificationDto(
    val id: String,
    val title: String,
    val content: String,
    val sender: String,
    val packageName: String,
    val timestamp: Long
)

data class SummaryRequest(
    val notifications: List<NotificationDto>
)

data class SummaryResponse(
    val summary: String,
    val suggestedReplies: List<String>
)
