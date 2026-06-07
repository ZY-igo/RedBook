package com.zhengyang.redbook.data.remote.interceptor

import java.io.IOException
import java.util.concurrent.ThreadLocalRandom
import okhttp3.Interceptor
import okhttp3.Response

class RetryInterceptor(
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
    private val baseDelayMillis: Long = DEFAULT_BASE_DELAY_MILLIS,
    private val maxDelayMillis: Long = DEFAULT_MAX_DELAY_MILLIS
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var lastIOException: IOException? = null
        repeat(maxRetries + 1) { attempt ->
            try {
                val response = chain.proceed(chain.request())
                if (!shouldRetry(response, attempt)) {
                    return response
                }
                sleepBeforeRetry(attempt, response.header(RETRY_AFTER_HEADER)?.toLongOrNull())
                response.close()
            } catch (error: IOException) {
                lastIOException = error
                if (attempt == maxRetries) throw error
                sleepBeforeRetry(attempt)
            }
        }
        throw lastIOException ?: IOException("Request failed after retry.")
    }

    private fun shouldRetry(response: Response, attempt: Int): Boolean {
        if (attempt == maxRetries || response.isSuccessful) return false
        if (response.code == TOO_MANY_REQUESTS) return true
        return response.code in RETRYABLE_SERVER_ERROR_RANGE
    }

    private fun sleepBeforeRetry(attempt: Int, retryAfterSeconds: Long? = null) {
        val retryAfterDelay = retryAfterSeconds?.takeIf { it > 0 }?.times(MILLIS_PER_SECOND)
        val exponentialDelay = (baseDelayMillis shl attempt).coerceAtMost(maxDelayMillis)
        val jitter = ThreadLocalRandom.current().nextLong(JITTER_BOUND_MILLIS)
        Thread.sleep((retryAfterDelay ?: exponentialDelay) + jitter)
    }

    private companion object {
        private const val DEFAULT_MAX_RETRIES = 2
        private const val DEFAULT_BASE_DELAY_MILLIS = 300L
        private const val DEFAULT_MAX_DELAY_MILLIS = 2_000L
        private const val JITTER_BOUND_MILLIS = 150L
        private const val MILLIS_PER_SECOND = 1_000L
        private const val TOO_MANY_REQUESTS = 429
        private const val RETRY_AFTER_HEADER = "Retry-After"
        private val RETRYABLE_SERVER_ERROR_RANGE = 500..599
    }
}
