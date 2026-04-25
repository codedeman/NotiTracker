package com.example.notitracker.data.remote.network

import java.lang.reflect.Type
import okhttp3.RequestBody

typealias QueryParams = Map<String, String?>

typealias HeaderMap = Map<String, String>

interface ApiRequest<out T : Any> {
    val method: HttpMethod
    val path: String

    val query: QueryParams get() = emptyMap()

    val headers: HeaderMap get() = emptyMap()

    val body: RequestBody? get() = null

    val responseType: Type
}
