package com.example.notitracker.data.repository

import com.example.notitracker.data.remote.dto.NotificationDto
import com.example.notitracker.data.remote.dto.SummaryResponse
import com.example.notitracker.data.remote.datasource.NotificationRemoteDataSourceImpl
import com.example.notitracker.data.remote.fake.FakeNetworkClient
import com.example.notitracker.data.remote.fake.FakeNotificationRemoteDataSource
import com.example.notitracker.data.remote.network.NetworkResult
import com.example.notitracker.data.remote.request.NotificationApi
import com.google.gson.Gson
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRepositoryImplTest {

    private val gson = Gson()
    private val notificationApi = NotificationApi(gson)

    @Test
    fun `getNotifications maps success to NetworkResponse Success`() = runBlocking {
        val fake = FakeNetworkClient().apply {
            stub(
                notificationApi.getNotifications(),
                NetworkResult.Success(
                    listOf(
                        NotificationDto("1", "t", "c", "s", "pkg", 0L),
                    ),
                ),
            )
        }
        val ds = NotificationRemoteDataSourceImpl(fake, notificationApi)
        val repo = NotificationRepositoryImpl(ds)

        val emissions = repo.getNotifications().take(2).toList()
        assertTrue(emissions[0] is com.example.notitracker.data.remote.NetworkResponse.Loading)
        assertTrue(emissions[1] is com.example.notitracker.data.remote.NetworkResponse.Success)
        val data = (emissions[1] as com.example.notitracker.data.remote.NetworkResponse.Success).data
        assertEquals(1, data.size)
        assertEquals("1", data.first().id)
    }

    @Test
    fun `getSummary returns success when stubbed`() = runBlocking {
        val fake = FakeNetworkClient().apply {
            stub(
                notificationApi.postSummary(
                    com.example.notitracker.data.remote.dto.SummaryRequest(emptyList()),
                ),
                NetworkResult.Success(SummaryResponse("hello", listOf("ok"))),
            )
        }
        val ds = NotificationRemoteDataSourceImpl(fake, notificationApi)
        val repo = NotificationRepositoryImpl(ds)

        val response = repo.getSummary(emptyList())
        assertTrue(response is com.example.notitracker.data.remote.NetworkResponse.Success)
        assertEquals("hello", (response as com.example.notitracker.data.remote.NetworkResponse.Success).data.summary)
    }

    @Test
    fun `repository works with fake data source`() = runBlocking {
        val fakeDs = FakeNotificationRemoteDataSource().apply {
            notificationsResult = NetworkResult.Success(emptyList())
        }
        val repo = NotificationRepositoryImpl(fakeDs)
        val emissions = repo.getNotifications().take(2).toList()
        assertTrue(emissions[1] is com.example.notitracker.data.remote.NetworkResponse.Success)
    }
}
