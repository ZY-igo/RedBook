/**
 * 文件说明：RetryInterceptor.kt
 * 作用：在可恢复的失败场景下对网络请求执行自动重试。
 * 备注：该拦截器适合处理短暂网络抖动、限流或服务端瞬时异常，不负责处理业务参数错误。
 */
package com.zhengyang.redbook.data.remote.interceptor

import java.net.ConnectException
import java.io.IOException
import java.util.concurrent.ThreadLocalRandom
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 请求重试拦截器。
 *
 * 当前支持两类重试条件：
 * 1. 请求过程中抛出 `IOException`，说明网络链路层失败。
 * 2. 服务端返回 429 或 5xx，说明请求可能因为限流或临时服务异常失败。
 *
 * 重试延迟采用“指数退避 + 随机抖动”策略，尽量减少大量请求同时重放带来的瞬时压力。
 */
class RetryInterceptor(
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
    private val baseDelayMillis: Long = DEFAULT_BASE_DELAY_MILLIS,
    private val maxDelayMillis: Long = DEFAULT_MAX_DELAY_MILLIS
) : Interceptor {

    /**
     * 执行带重试能力的请求分发。
     *
     * 总尝试次数为 `maxRetries + 1`，其中包含首次正常请求。
     * 每次命中可重试条件时，都会等待一段时间再发起下一次请求。
     */
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
                if (!shouldRetry(error, attempt)) throw error
                sleepBeforeRetry(attempt)
            }
        }
        throw lastIOException ?: IOException("Request failed after retry.")
    }

    /**
     * 判断当前响应是否值得重试。
     *
     * 成功响应、已达到最大重试次数的响应，或明显属于客户端错误的状态码，都不会继续重试。
     */
    private fun shouldRetry(response: Response, attempt: Int): Boolean {
        if (attempt == maxRetries || response.isSuccessful) return false
        if (response.code == TOO_MANY_REQUESTS) return true
        return response.code in RETRYABLE_SERVER_ERROR_RANGE
    }

    /**
     * 判断异常是否值得重试。
     *
     * 对于连接被拒绝这类“目标端口当前无人监听”的错误，继续重试几乎没有价值，
     * 直接失败可以更快暴露配置或后端未启动的问题。
     */
    private fun shouldRetry(error: IOException, attempt: Int): Boolean {
        if (attempt == maxRetries) return false
        return error !is ConnectException
    }

    /**
     * 在两次请求之间等待。
     *
     * 优先使用服务端的 `Retry-After` 头；若后端未提供，则退回到本地指数退避策略。
     * 最终再叠加一个轻量随机抖动，避免多个请求在同一时刻同时重试。
     */
    private fun sleepBeforeRetry(attempt: Int, retryAfterSeconds: Long? = null) {
        val retryAfterDelay = retryAfterSeconds?.takeIf { it > 0 }?.times(MILLIS_PER_SECOND)
        val exponentialDelay = (baseDelayMillis shl attempt).coerceAtMost(maxDelayMillis)
        val jitter = ThreadLocalRandom.current().nextLong(JITTER_BOUND_MILLIS)
        Thread.sleep((retryAfterDelay ?: exponentialDelay) + jitter)
    }

    private companion object {
        /** 默认最大重试次数，不含第一次正常请求。 */
        private const val DEFAULT_MAX_RETRIES = 2

        /** 指数退避的基础等待时长。 */
        private const val DEFAULT_BASE_DELAY_MILLIS = 300L

        /** 单次等待时长上限，避免指数退避无限增大。 */
        private const val DEFAULT_MAX_DELAY_MILLIS = 2_000L

        /** 随机抖动上限，用于打散并发请求的重试时刻。 */
        private const val JITTER_BOUND_MILLIS = 150L

        /** 秒转毫秒的换算常量。 */
        private const val MILLIS_PER_SECOND = 1_000L

        /** HTTP 429，通常表示服务端限流。 */
        private const val TOO_MANY_REQUESTS = 429

        /** 服务端建议的重试等待头。 */
        private const val RETRY_AFTER_HEADER = "Retry-After"

        /** 认为可重试的服务端错误状态码区间。 */
        private val RETRYABLE_SERVER_ERROR_RANGE = 500..599
    }
}
