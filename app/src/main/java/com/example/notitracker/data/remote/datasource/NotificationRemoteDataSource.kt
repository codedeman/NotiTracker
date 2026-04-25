package com.example.notitracker.data.remote.datasource

import com.example.notitracker.data.remote.dto.*
import com.example.notitracker.data.remote.network.NetworkClient
import com.example.notitracker.data.remote.network.NetworkResult
import com.example.notitracker.data.remote.request.NotificationApi

interface NotificationRemoteDataSource {
    suspend fun getNotifications(): NetworkResult<List<NotificationDto>>
    suspend fun postChatCompletion(messages: List<ChatMessage>): NetworkResult<ChatResponse>
}

class NotificationRemoteDataSourceImpl(
    private val networkClient: NetworkClient,
    private val notificationApi: NotificationApi,
) : NotificationRemoteDataSource {

    override suspend fun getNotifications(): NetworkResult<List<NotificationDto>> =
        networkClient.execute(notificationApi.getNotifications())

    override suspend fun postChatCompletion(messages: List<ChatMessage>): NetworkResult<ChatResponse> =
        networkClient.execute(notificationApi.postChatCompletion(messages))
}
