package com.example.notitracker.data.remote.network

import com.example.notitracker.data.remote.NetworkResponse

fun <T> NetworkResult<T>.toNetworkResponse(): NetworkResponse<T> = when (this) {
    is NetworkResult.Success -> NetworkResponse.Success(data)
    is NetworkResult.Failure -> NetworkResponse.Error(
        message = error.toUserMessage(),
        code = (error as? AppError.Http)?.code,
    )
}
