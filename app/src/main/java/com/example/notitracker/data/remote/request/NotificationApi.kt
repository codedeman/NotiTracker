package com.example.notitracker.data.remote.request

import com.example.notitracker.data.remote.dto.*
import com.example.notitracker.data.remote.network.ApiRequest
import com.example.notitracker.data.remote.network.HttpMethod
import com.example.notitracker.data.remote.network.JsonMediaType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import okhttp3.RequestBody.Companion.toRequestBody

class NotificationApi(
    private val gson: Gson,
) {
    fun getNotifications(): ApiRequest<List<NotificationDto>> = GetNotificationsRequest()

    fun postChatCompletion(messages: List<ChatMessage>): ApiRequest<ChatResponse> {
        val body = ChatRequest(messages = messages)
        return PostChatCompletionRequest(body, gson)
    }
}

private class GetNotificationsRequest : ApiRequest<List<NotificationDto>> {
    override val method = HttpMethod.GET
    override val path = "notifications"
    override val responseType: Type =
        object : TypeToken<List<NotificationDto>>() {}.type
}

private class PostChatCompletionRequest(
    private val payload: ChatRequest,
    private val gson: Gson,
) : ApiRequest<ChatResponse> {
    override val method = HttpMethod.POST
    // Endpoint từ cURL của bạn
    override val path = "v1/chat/completions"
    override val body = gson.toJson(payload).toRequestBody(JsonMediaType)
    override val responseType: Type = ChatResponse::class.java
}
