package com.example.notitracker.data.remote.network

sealed class AppError(open val message: String? = null) {
    data class Http(val code: Int, override val message: String? = null) : AppError(message)

    data class Network(override val message: String? = null) : AppError(message)

    data class Serialization(override val message: String? = null) : AppError(message)

    data class Unauthorized(override val message: String? = null) : AppError(message)

    data class Unknown(override val message: String? = null) : AppError(message)

    fun toUserMessage(): String = message ?: when (this) {
        is Http -> "Server error ($code)"
        is Network -> "Network error"
        is Serialization -> "Invalid response"
        is Unauthorized -> "Session expired"
        is Unknown -> "Something went wrong"
    }
}
