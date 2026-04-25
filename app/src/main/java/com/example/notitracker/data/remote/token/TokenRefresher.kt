package com.example.notitracker.data.remote.token

fun interface TokenRefresher {
    suspend fun refresh(): Boolean
}
