package com.example.notitracker.data.repository

import com.example.notitracker.data.remote.dto.UserDto
import com.example.notitracker.data.remote.network.NetworkClient
import com.example.notitracker.data.remote.network.NetworkResult
import com.example.notitracker.data.remote.request.UserApi
import com.example.notitracker.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface UserRepository {
    fun getUser(userId: String): Flow<NetworkResult<User>>
}

class UserRepositoryImpl(
    private val networkClient: NetworkClient,
    private val userApi: UserApi,
) : UserRepository {

    override fun getUser(userId: String): Flow<NetworkResult<User>> = flow {
        when (val result = networkClient.execute(userApi.getUser(userId))) {
            is NetworkResult.Success -> emit(NetworkResult.Success(result.data.toDomain()))
            is NetworkResult.Failure -> emit(result)
        }
    }
}

private fun UserDto.toDomain() = User(
    id = id,
    name = name,
    email = email,
)
