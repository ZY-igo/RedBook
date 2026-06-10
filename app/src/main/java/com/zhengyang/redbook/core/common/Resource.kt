/**
 * 文件说明：Resource.kt
 * 作用：定义统一的资源状态封装类，用于表示数据请求的不同状态。
 * 备注：采用密封接口设计，确保类型安全。
 */
package com.zhengyang.redbook.core.common

/**
 * 资源状态密封接口。
 * 
 * 用于封装数据请求的三种状态：
 * - Loading：加载中
 * - Success：成功
 * - Error：失败
 * 
 * @param T 数据类型
 */
sealed interface Resource<out T> {

    /**
     * 加载状态。
     * 
     * 表示数据正在加载中，可能包含之前的缓存数据。
     * 
     * @param data 可选的缓存数据，在加载过程中可用于展示旧数据。
     */
    data class Loading<T>(val data: T? = null) : Resource<T>

    /**
     * 成功状态。
     * 
     * 表示数据请求成功，包含完整的数据。
     * 
     * @param data 请求成功返回的数据。
     */
    data class Success<T>(val data: T) : Resource<T>

    /**
     * 错误状态。
     * 
     * 表示数据请求失败，包含错误信息和异常对象。
     * 
     * @param throwable 导致失败的异常对象。
     * @param message 错误消息（默认使用异常的 message，若为空则使用 "Unknown error"）。
     * @param data 可选的缓存数据，在请求失败时可用于展示旧数据。
     */
    data class Error<T>(
        val throwable: Throwable,
        val message: String = throwable.message ?: "Unknown error",
        val data: T? = null
    ) : Resource<T>
}

/**
 * 将 Throwable 转换为用户友好的错误消息。
 * 
 * @param defaultMessage 默认错误消息，当 Throwable 的 message 为空时使用。
 * @return 用户友好的错误消息。
 */
fun Throwable.toUserMessage(defaultMessage: String): String {
    // 如果异常有消息且不为空，则返回该消息；否则返回默认消息
    return message?.takeIf(String::isNotBlank) ?: defaultMessage
}