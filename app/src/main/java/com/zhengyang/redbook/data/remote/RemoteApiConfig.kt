/**
 * 文件说明：RemoteApiConfig.kt
 * 作用：集中声明 Remote Api Config 相关配置项与常量。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.BuildConfig
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * 远程接口配置
 *
 * 负责向 OkHttp 和 Retrofit 提供统一的网络访问参数，
 * 包括基础地址、超时时间以及默认请求头等。
 */
class RemoteApiConfig @Inject constructor() {

    /** 当前环境下的后端基础地址。 */
    val baseUrl: HttpUrl = BuildConfig.BASE_API_URL.toHttpUrl()

    /** 连接超时时间，单位为秒。 */
    val connectTimeoutSeconds: Long = BuildConfig.API_CONNECT_TIMEOUT_SECONDS.toLong()

    /** 读取超时时间，单位为秒。 */
    val readTimeoutSeconds: Long = BuildConfig.API_READ_TIMEOUT_SECONDS.toLong()

    /** 写入超时时间，单位为秒。 */
    val writeTimeoutSeconds: Long = BuildConfig.API_WRITE_TIMEOUT_SECONDS.toLong()

    /** 默认请求头集合，所有接口请求都会复用该配置。 */
    val defaultHeaders: Map<String, String> = mapOf(
        HEADER_ACCEPT to "application/json",
        HEADER_CONTENT_TYPE to "application/json"
    )

    /**
     * 获取连接超时对应的时间单位。
     *
     * @return 秒级时间单位
     */
    fun connectTimeoutUnit(): TimeUnit = TimeUnit.SECONDS

    /**
     * 获取读取超时对应的时间单位。
     *
     * @return 秒级时间单位
     */
    fun readTimeoutUnit(): TimeUnit = TimeUnit.SECONDS

    /**
     * 获取写入超时对应的时间单位。
     *
     * @return 秒级时间单位
     */
    fun writeTimeoutUnit(): TimeUnit = TimeUnit.SECONDS

    companion object {
        /** 默认的 Accept 请求头键名。 */
        const val HEADER_ACCEPT = "Accept"

        /** 默认的 Content-Type 请求头键名。 */
        const val HEADER_CONTENT_TYPE = "Content-Type"
    }
}
