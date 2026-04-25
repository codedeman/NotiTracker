package com.example.notitracker.data.remote.network

import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class OkHttpNetworkClient(
    private val client: OkHttpClient,
    private val baseUrl: HttpUrl,
    private val gson: Gson,
) : NetworkClient {

    override suspend fun <T : Any> execute(request: ApiRequest<T>): NetworkResult<T> =
        withContext(Dispatchers.IO) {
            val url = buildUrl(request) ?: return@withContext NetworkResult.Failure(
                AppError.Unknown("Invalid URL"),
            )

            val builder = Request.Builder().url(url)
            request.headers.forEach { (k, v) -> builder.addHeader(k, v) }
            when (request.method) {
                HttpMethod.GET -> builder.get()
                HttpMethod.DELETE -> {
                    if (request.body != null) builder.delete(request.body)
                    else builder.delete()
                }
                HttpMethod.POST -> builder.post(request.body ?: error("POST requires body"))
                HttpMethod.PUT -> builder.put(request.body ?: error("PUT requires body"))
            }

            val call = client.newCall(builder.build())
            coroutineContext[Job]?.invokeOnCompletion { cause ->
                if (cause != null) call.cancel()
            }

            try {
                ensureActive()
                val response = call.execute()
                mapResponse(request, response)
            } catch (e: IOException) {
                NetworkResult.Failure(e.toAppError())
            }
        }

    private fun buildUrl(request: ApiRequest<*>): HttpUrl? {
        val resolved = baseUrl.resolve(request.path.trimStart('/')) ?: return null
        if (request.query.isEmpty()) return resolved
        val builder = resolved.newBuilder()
        request.query.forEach { (key, value) ->
            if (value != null) builder.addQueryParameter(key, value)
        }
        return builder.build()
    }

    private fun <T : Any> mapResponse(request: ApiRequest<T>, response: Response): NetworkResult<T> {
        response.use {
            if (!response.isSuccessful) {
                val body = response.body?.string()
                return NetworkResult.Failure(mapHttpToAppError(response.code, body))
            }
            val source = response.body?.charStream() ?: return NetworkResult.Failure(
                AppError.Serialization("Empty body"),
            )
            return try {
                @Suppress("UNCHECKED_CAST")
                val data = gson.fromJson<T>(source, request.responseType) as T
                NetworkResult.Success(data)
            } catch (e: Exception) {
                NetworkResult.Failure(AppError.Serialization(e.message))
            }
        }
    }
}

fun String.toHttpUrlOrThrow(): HttpUrl =
    toHttpUrlOrNull() ?: error("Invalid base URL: $this")
