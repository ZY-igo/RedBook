/**
 * 文件说明：RemoteApiConfig.kt
 * 作用：集中维护 remote 层网络访问相关的基础配置。
 * 备注：该类把 BuildConfig 中的接口环境参数封装成统一对象，避免调用方分散读取。
 */
package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.BuildConfig
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * 远程接口配置。
 *
 * 主要职责：
 * 1. 提供统一的 baseUrl。
 * 2. 提供 OkHttp 连接、读取、写入超时配置。
 * 3. 提供全局默认请求头。
 *
 * 通过这一层收口后，remote 相关组件就不需要直接依赖 `BuildConfig` 细节。
 */
class RemoteApiConfig @Inject constructor() {

    /** 当前环境下的后端基础地址，所有相对路径接口都会基于它解析。 */
    val baseUrl: HttpUrl = BuildConfig.BASE_API_URL.toHttpUrl()

    /** 连接超时时间，控制建连阶段的最长等待时长，单位为秒。 */
    val connectTimeoutSeconds: Long = BuildConfig.API_CONNECT_TIMEOUT_SECONDS.toLong()

    /** 读取超时时间，控制读取响应内容时的最长等待时长，单位为秒。 */
    val readTimeoutSeconds: Long = BuildConfig.API_READ_TIMEOUT_SECONDS.toLong()

    /** 写入超时时间，控制上传请求体时的最长等待时长，单位为秒。 */
    val writeTimeoutSeconds: Long = BuildConfig.API_WRITE_TIMEOUT_SECONDS.toLong()

    /**
     * 默认请求头集合。
     *
     * 这里只放稳定的协议级请求头；鉴权头、实验头或业务自定义头
     * 更适合由上层拦截器在具体场景下注入。
     */
    val defaultHeaders: Map<String, String> = mapOf(
        HEADER_ACCEPT to "application/json",
        HEADER_CONTENT_TYPE to "application/json"
    )

    /** 返回连接超时使用的时间单位，供 OkHttp Builder 配置时直接复用。 */
    fun connectTimeoutUnit(): TimeUnit = TimeUnit.SECONDS

    /** 返回读取超时使用的时间单位，供 OkHttp Builder 配置时直接复用。 */
    fun readTimeoutUnit(): TimeUnit = TimeUnit.SECONDS

    /** 返回写入超时使用的时间单位，供 OkHttp Builder 配置时直接复用。 */
    fun writeTimeoutUnit(): TimeUnit = TimeUnit.SECONDS

    companion object {
        /** 默认 `Accept` 请求头键名。 */
        const val HEADER_ACCEPT = "Accept"

        /** 默认 `Content-Type` 请求头键名。 */
        const val HEADER_CONTENT_TYPE = "Content-Type"
    }
}
