/**
 * 文件说明： RemoteApiConfig.kt
 * 作用： 封装远程数据访问相关逻辑，包括接口配置、请求行为和响应解析。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.BuildConfig
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

class RemoteApiConfig @Inject constructor() {
    val baseUrl: HttpUrl = BuildConfig.BASE_API_URL.toHttpUrl()
    val listContentPath: String = "api/v1/home/feed?categoryId=recommend&offset=0&limit=10"
    val useMockData: Boolean = BuildConfig.USE_MOCK_DATA
    val connectTimeoutSeconds: Long = BuildConfig.API_CONNECT_TIMEOUT_SECONDS.toLong()
    val readTimeoutSeconds: Long = BuildConfig.API_READ_TIMEOUT_SECONDS.toLong()
    val writeTimeoutSeconds: Long = BuildConfig.API_WRITE_TIMEOUT_SECONDS.toLong()

    val defaultHeaders: Map<String, String> = mapOf(
        HEADER_ACCEPT to "application/json",
        HEADER_CONTENT_TYPE to "application/json"
    )

    fun shouldUseMockData(): Boolean = useMockData

    fun connectTimeoutUnit(): TimeUnit = TimeUnit.SECONDS
    fun readTimeoutUnit(): TimeUnit = TimeUnit.SECONDS
    fun writeTimeoutUnit(): TimeUnit = TimeUnit.SECONDS

    companion object {
        const val HEADER_ACCEPT = "Accept"
        const val HEADER_CONTENT_TYPE = "Content-Type"
    }
}
