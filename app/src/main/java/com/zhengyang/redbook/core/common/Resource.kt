package com.zhengyang.redbook.core.common

sealed interface Resource<out T> {
    data class Loading<T>(val data: T? = null) : Resource<T>

    data class Success<T>(val data: T) : Resource<T>

    data class Error<T>(
        val throwable: Throwable,
        val message: String = throwable.message ?: "Unknown error",
        val data: T? = null
    ) : Resource<T>
}

fun Throwable.toUserMessage(defaultMessage: String): String {
    return message?.takeIf(String::isNotBlank) ?: defaultMessage
}
