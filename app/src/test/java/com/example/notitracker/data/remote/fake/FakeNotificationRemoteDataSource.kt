package com.example.notitracker.data.remote.fake

import com.example.notitracker.data.remote.dto.NotificationDto
import com.example.notitracker.data.remote.dto.SummaryRequest
import com.example.notitracker.data.remote.dto.SummaryResponse
import com.example.notitracker.data.remote.datasource.NotificationRemoteDataSource
import com.example.notitracker.data.remote.network.NetworkResult

/**
 * Stub the data source when tests should not depend on [NetworkClient] / HTTP details.
 */
class FakeNotificationRemoteDataSource : NotificationRemoteDataSource {

    var notificationsResult: NetworkResult<List<NotificationDto>> =
        NetworkResult.Success(emptyList())

    var summaryResult: NetworkResult<SummaryResponse> =
        NetworkResult.Success(SummaryResponse("", emptyList()))

    override suspend fun getNotifications(): NetworkResult<List<NotificationDto>> =
        notificationsResult

    override suspend fun postSummary(request: SummaryRequest): NetworkResult<SummaryResponse> =
        summaryResult
}
