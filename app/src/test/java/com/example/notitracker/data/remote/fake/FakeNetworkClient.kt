package com.example.notitracker.data.remote.fake

import com.example.notitracker.data.remote.network.ApiRequest
import com.example.notitracker.data.remote.network.AppError
import com.example.notitracker.data.remote.network.NetworkClient
import com.example.notitracker.data.remote.network.NetworkResult

class FakeNetworkClient : NetworkClient {

    private val stubs = mutableMapOf<String, NetworkResult<*>>()

    fun <T : Any> stub(request: ApiRequest<T>, result: NetworkResult<T>) {
        stubs[key(request)] = result
    }

    fun clear() {
        stubs.clear()
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> execute(request: ApiRequest<T>): NetworkResult<T> {
        val key = key(request)
        val hit = stubs[key] as NetworkResult<T>?
        return hit ?: NetworkResult.Failure(AppError.Unknown("Unstubbed request: $key"))
    }

    private fun key(request: ApiRequest<*>): String =
        "${request.method}:${request.path}"
}
