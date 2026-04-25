package com.example.notitracker.data.remote.interceptor

import com.example.notitracker.data.remote.token.TokenProvider
import okhttp3.Interceptor
import okhttp3.Response

class BearerAuthInterceptor(
    private val tokenProvider: TokenProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.accessToken() ?: return chain.proceed(chain.request())
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}
