package com.example.notitracker.data.repository

import com.example.notitracker.data.remote.ApiService
import com.example.notitracker.data.remote.NetworkResponse
import com.example.notitracker.data.remote.NotificationDto
import com.example.notitracker.data.remote.SummaryRequest
import com.example.notitracker.data.remote.SummaryResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface NotificationRepository {
    fun getNotifications(): Flow<NetworkResponse<List<NotificationDto>>>
    suspend fun getSummary(notifications: List<NotificationDto>): NetworkResponse<SummaryResponse>
}

class NotificationRepositoryImpl(
    private val apiService: ApiService
) : NotificationRepository {

    override fun getNotifications(): Flow<NetworkResponse<List<NotificationDto>>> = flow {
        emit(NetworkResponse.Loading)
        try {
            val response = apiService.getNotifications()
            if (response.isSuccessful) {
                emit(NetworkResponse.Success(response.body() ?: emptyList()))
            } else {
                emit(NetworkResponse.Error("API error: ${response.message()}", response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResponse.Error(e.message ?: "Unknown error"))
        }
    }

    override suspend fun getSummary(notifications: List<NotificationDto>): NetworkResponse<SummaryResponse> {
        return try {
            val response = apiService.getSummary(SummaryRequest(notifications))
            if (response.isSuccessful) {
                NetworkResponse.Success(response.body()!!)
            } else {
                NetworkResponse.Error("Summary API error: ${response.message()}", response.code())
            }
        } catch (e: Exception) {
            NetworkResponse.Error(e.message ?: "Unknown error")
        }
    }
}
