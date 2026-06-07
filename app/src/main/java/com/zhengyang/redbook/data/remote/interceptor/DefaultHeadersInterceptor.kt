package com.zhengyang.redbook.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class DefaultHeadersInterceptor(
    private val headers: Map<String, String>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        headers.forEach { (key, value) ->
            if (chain.request().header(key).isNullOrBlank()) {
                requestBuilder.header(key, value)
            }
        }
        return chain.proceed(requestBuilder.build())
    }
}
