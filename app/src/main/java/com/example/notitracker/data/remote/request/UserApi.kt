package com.example.notitracker.data.remote.request

import com.example.notitracker.data.remote.dto.UserDto
import com.example.notitracker.data.remote.network.ApiRequest
import com.example.notitracker.data.remote.network.HttpMethod
import java.lang.reflect.Type

class UserApi {
    fun getUser(userId: String): ApiRequest<UserDto> = GetUserRequest(userId)
}

private class GetUserRequest(
    private val userId: String,
) : ApiRequest<UserDto> {
    override val method = HttpMethod.GET
    override val path get() = "users/$userId"
    override val responseType: Type = UserDto::class.java
}
