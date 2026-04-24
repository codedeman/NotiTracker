package com.example.notitracker.data.remote.token

fun interface TokenProvider {
    fun accessToken(): String?
}
