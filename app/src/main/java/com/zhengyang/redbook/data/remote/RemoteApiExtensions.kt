/**
 * 文件说明：RemoteApiExtensions.kt
 * 作用：提供 Remote Api Extensions 相关扩展方法与辅助封装。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.data.remote.model.ApiResponseDto

class RemoteApiException(message: String) : IllegalStateException(message)

fun <T> ApiResponseDto<T>.requireData(): T {
    if (success && data != null) return data
    throw RemoteApiException(message.ifBlank { code.ifBlank { "Remote api call failed." } })
}
