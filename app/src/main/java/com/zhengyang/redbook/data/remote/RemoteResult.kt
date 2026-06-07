/**
 * 文件说明：RemoteResult.kt
 * 作用：定义 Remote Result 场景中的结果状态与数据封装模型。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.remote

sealed interface RemoteResult<out T> {
    data class Success<T>(val value: T) : RemoteResult<T>

    data class HttpError(
        val code: Int,
        val body: String
    ) : RemoteResult<Nothing>

    data class NetworkError(val cause: Throwable) : RemoteResult<Nothing>

    data class ParseError(val cause: Throwable) : RemoteResult<Nothing>
}
