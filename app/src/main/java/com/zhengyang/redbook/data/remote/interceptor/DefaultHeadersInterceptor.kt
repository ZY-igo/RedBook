/**
 * 文件说明：DefaultHeadersInterceptor.kt
 * 作用：为请求补齐一组全局默认请求头。
 * 备注：该拦截器只做“缺失时补充”，不会覆盖调用方已经显式设置的同名 header。
 */
package com.zhengyang.redbook.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 默认请求头拦截器。
 *
 * 适用于为所有接口统一补充诸如 `Accept`、`Content-Type` 这类稳定协议头。
 * 若某个请求已经自行设置了同名 header，则优先尊重请求本身的值。
 */
class DefaultHeadersInterceptor(
    private val headers: Map<String, String>
) : Interceptor {

    /**
     * 在请求发出前补齐默认请求头。
     *
     * 采用“只补不盖”的策略，避免全局配置意外覆盖局部业务请求的特殊 header。
     */
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
