package com.example.notitracker.data.remote.network

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>

    data class Failure(val error: AppError) : NetworkResult<Nothing>
}
