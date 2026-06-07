/**
 * 文件说明：DefaultHeadersInterceptor.kt
 * 作用：处理网络请求链路中的 Default Headers Interceptor 相关拦截逻辑。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
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
