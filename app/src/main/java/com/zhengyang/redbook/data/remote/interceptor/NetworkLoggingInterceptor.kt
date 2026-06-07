package com.zhengyang.redbook.data.remote.interceptor

import com.zhengyang.redbook.utils.AppLogger
import okhttp3.Interceptor
import okhttp3.Response

class NetworkLoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNs = System.nanoTime()
        AppLogger.d("Network", "-> ${request.method} ${request.url}")
        return try {
            val response = chain.proceed(request)
            val durationMs = (System.nanoTime() - startNs) / 1_000_000
            AppLogger.d(
                "Network",
                "<- ${response.code} ${request.method} ${request.url} (${durationMs}ms)"
            )
            response
        } catch (throwable: Throwable) {
            val durationMs = (System.nanoTime() - startNs) / 1_000_000
            AppLogger.e(
                "Network",
                "<- failed ${request.method} ${request.url} (${durationMs}ms)",
                throwable
            )
            throw throwable
        }
    }
}
