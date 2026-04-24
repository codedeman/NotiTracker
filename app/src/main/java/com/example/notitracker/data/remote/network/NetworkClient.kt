package com.example.notitracker.data.remote.network

interface NetworkClient {
    suspend fun <T : Any> execute(request: ApiRequest<T>): NetworkResult<T>
}
