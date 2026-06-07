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
