package com.example.notitracker.data.remote.network

import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException

fun Throwable.toAppError(): AppError = when (this) {
    is CancellationException -> throw this
    is SocketTimeoutException,
    is IOException,
    -> AppError.Network(message)
    else -> AppError.Unknown(message)
}

fun mapHttpToAppError(code: Int, body: String?): AppError = when (code) {
    401 -> AppError.Unauthorized(body)
    in 400..499 -> AppError.Http(code, body)
    in 500..599 -> AppError.Http(code, body)
    else -> AppError.Http(code, body)
}
