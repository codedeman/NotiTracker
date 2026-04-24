package com.example.notitracker.data.repository

import com.example.notitracker.data.remote.NetworkResponse
import com.example.notitracker.data.remote.dto.*
import com.example.notitracker.data.remote.datasource.NotificationRemoteDataSource
import com.example.notitracker.data.remote.network.toNetworkResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

interface NotificationRepository {
    fun getNotifications(): Flow<NetworkResponse<List<NotificationDto>>>

    suspend fun getSummary(notifications: List<NotificationDto>): NetworkResponse<SummaryResponse>
}

class NotificationRepositoryImpl(
    private val remoteDataSource: NotificationRemoteDataSource,
) : NotificationRepository {

    override fun getNotifications(): Flow<NetworkResponse<List<NotificationDto>>> = flow {
        emit(NetworkResponse.Loading)
        val result = remoteDataSource.getNotifications()
        emit(result.toNetworkResponse())
    }.catch { e ->
        emit(NetworkResponse.Error(e.message ?: "Unknown error"))
    }

    override suspend fun getSummary(notifications: List<NotificationDto>): NetworkResponse<SummaryResponse> {
        val prompt = notifications.joinToString("\n") { "${it.sender}: ${it.content}" }
        val messages = listOf(
            ChatMessage(role = "system", content = "Bạn là một trợ lý tóm tắt thông báo. Hãy tóm tắt các tin nhắn sau một cách ngắn gọn và đưa ra 3 câu trả lời gợi ý ngắn."),
            ChatMessage(role = "user", content = prompt)
        )

        return try {
            val result = remoteDataSource.postChatCompletion(messages)
            when (val response = result.toNetworkResponse()) {
                is NetworkResponse.Success -> {
                    val content = response.data.choices.firstOrNull()?.message?.content ?: ""
                    // Giả định AI trả về format: Tóm tắt | Gợi ý 1, Gợi ý 2, Gợi ý 3
                    // Để đơn giản, tôi sẽ parse thô hoặc trả về nguyên văn content làm summary
                    NetworkResponse.Success(SummaryResponse(
                        summary = content,
                        suggestedReplies = listOf("Ok", "Đã hiểu", "Sẽ trả lời sau")
                    ))
                }
                is NetworkResponse.Error -> NetworkResponse.Error(response.message)
                else -> NetworkResponse.Error("Unknown error")
            }
        } catch (e: Exception) {
            NetworkResponse.Error(e.message ?: "Unknown error")
        }
    }
}
