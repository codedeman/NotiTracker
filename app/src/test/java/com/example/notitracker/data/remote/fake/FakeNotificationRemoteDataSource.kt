package com.example.notitracker.data.remote.fake

import com.example.notitracker.data.remote.dto.ChatChoice
import com.example.notitracker.data.remote.dto.ChatMessage
import com.example.notitracker.data.remote.dto.ChatResponse
import com.example.notitracker.data.remote.dto.NotificationDto
import com.example.notitracker.data.remote.datasource.NotificationRemoteDataSource
import com.example.notitracker.data.remote.network.NetworkResult
import org.junit.Test

class FakeNotificationRemoteDataSource : NotificationRemoteDataSource {

    var notificationsResult: NetworkResult<List<NotificationDto>> =
        NetworkResult.Success(emptyList())

    // Nạp dữ liệu Stub từ response thực của bạn
    var chatCompletionResult: NetworkResult<ChatResponse> =
        NetworkResult.Success(
            ChatResponse(
                id = "chatcmpl-vtgot05j478z1wfu6ylx6",
                choices = listOf(
                    ChatChoice(
                        message = ChatMessage(
                            role = "assistant", 
                            content = "Chào bạn! Rất vui được gặp bạn. Tôi là Qwen, một trợ lý AI được phát triển bởi Alibaba Cloud..."
                        ),
                    ),
                ),
            )
        )

    override suspend fun getNotifications(): NetworkResult<List<NotificationDto>> =
        notificationsResult

    override suspend fun postChatCompletion(messages: List<ChatMessage>): NetworkResult<ChatResponse> =
        chatCompletionResult
}
