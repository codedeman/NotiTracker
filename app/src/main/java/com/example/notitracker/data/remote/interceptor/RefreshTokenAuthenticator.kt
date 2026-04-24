package com.example.notitracker.data.remote.interceptor

import com.example.notitracker.data.remote.token.TokenProvider
import com.example.notitracker.data.remote.token.TokenRefresher
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * On 401, attempts [TokenRefresher.refresh] then retries with a new Authorization header.
 * Runs on OkHttp worker threads; [runBlocking] is acceptable here if the refresher does not
 * call back into the same OkHttpClient without a dedicated refresh client.
 */
class RefreshTokenAuthenticator(
    private val tokenProvider: TokenProvider,
    private val tokenRefresher: TokenRefresher,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization") == null) return null
        synchronized(this) {
            val refreshed = runBlocking { tokenRefresher.refresh() }
            if (!refreshed) return null
            val token = tokenProvider.accessToken() ?: return null
            return response.request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
    }
}
