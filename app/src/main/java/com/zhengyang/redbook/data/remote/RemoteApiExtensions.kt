package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.data.remote.model.ApiResponseDto

class RemoteApiException(message: String) : IllegalStateException(message)

fun <T> ApiResponseDto<T>.requireData(): T {
    if (success && data != null) return data
    throw RemoteApiException(message.ifBlank { code.ifBlank { "Remote api call failed." } })
}
