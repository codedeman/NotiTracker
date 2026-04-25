package com.example.notitracker.data.repository

import com.example.notitracker.data.remote.dto.UserDto
import com.example.notitracker.data.remote.fake.FakeNetworkClient
import com.example.notitracker.data.remote.network.NetworkResult
import com.example.notitracker.data.remote.request.UserApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRepositoryImplTest {

    @Test
    fun `getUser maps dto to domain on success`() = runBlocking {
        val userApi = UserApi()
        val fake = FakeNetworkClient().apply {
            stub(
                userApi.getUser("42"),
                NetworkResult.Success(UserDto("42", "Pat", "pat@example.com")),
            )
        }
        val repo = UserRepositoryImpl(fake, userApi)

        val result = repo.getUser("42").first()
        assertTrue(result is NetworkResult.Success)
        val user = (result as NetworkResult.Success).data
        assertEquals(User("42", "Pat", "pat@example.com"), user)
    }

    @Test
    fun `getUser propagates failure`() = runBlocking {
        val userApi = UserApi()
        val fake = FakeNetworkClient()
        val repo = UserRepositoryImpl(fake, userApi)

        val result = repo.getUser("99").first()
        assertTrue(result is NetworkResult.Failure)
    }
}
