package com.example.notitracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NotificationDto(
    val id: String,
    val title: String,
    val content: String,
    val sender: String,
    val packageName: String,
    val timestamp: Long,
)

// --- OpenAI / DeepSeek Request Objects ---
data class ChatRequest(
    val model: String = "deepseek-r1-distill-qwen-1.5b",
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens") val maxTokens: Int = -1,
    val stream: Boolean = false
)

data class ChatMessage(
    val role: String,
    val content: String
)

// --- OpenAI / DeepSeek Response Objects ---
data class ChatResponse(
    val id: String,
    val choices: List<ChatChoice>
)

data class ChatChoice(
    val message: ChatMessage
)

// --- Legacy support for existing code ---
data class SummaryRequest(
    val notifications: List<NotificationDto>,
)

data class SummaryResponse(
    val summary: String,
    val suggestedReplies: List<String>,
)
