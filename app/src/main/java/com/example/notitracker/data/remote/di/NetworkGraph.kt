package com.example.notitracker.data.remote.di

import android.content.Context
import com.example.notitracker.data.remote.datasource.NotificationRemoteDataSource
import com.example.notitracker.data.remote.datasource.NotificationRemoteDataSourceImpl
import com.example.notitracker.data.remote.network.OkHttpNetworkClient
import com.example.notitracker.data.remote.network.toHttpUrlOrThrow
import com.example.notitracker.data.remote.request.NotificationApi
import com.example.notitracker.data.repository.NotificationRepository
import com.example.notitracker.data.repository.NotificationRepositoryImpl
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class NetworkGraph private constructor(context: Context) {
    private val gson = Gson()
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val notificationApi = NotificationApi(gson)

    private val networkClient = OkHttpNetworkClient(
        client = okHttpClient,
        baseUrl = DEFAULT_BASE_URL.toHttpUrlOrThrow(),
        gson = gson,
    )

    private val notificationRemoteDataSource: NotificationRemoteDataSource =
        NotificationRemoteDataSourceImpl(networkClient, notificationApi)

    val notificationRepository: NotificationRepository =
        NotificationRepositoryImpl(notificationRemoteDataSource)

    companion object {
        private const val DEFAULT_BASE_URL = "https://0349-42-114-234-75.ngrok-free.app/"

        fun create(context: Context): NetworkGraph {
            return NetworkGraph(context)
        }
    }
}
