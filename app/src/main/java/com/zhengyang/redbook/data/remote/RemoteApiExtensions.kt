/**
 * 文件说明：RemoteApiExtensions.kt
 * 作用：提供 remote 层常用的扩展函数和异常类型。
 * 备注：这些能力用于减少 repository 在解析统一响应壳时的样板代码。
 */
package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.data.remote.model.ApiResponseDto

/**
 * 远端接口业务异常。
 *
 * 它表示“HTTP 调用完成了，但服务端业务结果不可用”这类情况，
 * 例如 `success=false`、`data` 缺失或后端返回了业务错误信息。
 */
class RemoteApiException(message: String) : IllegalStateException(message)

/**
 * 从统一响应壳中提取必需的业务数据。
 *
 * 使用约定：
 * 1. 当 `success=true` 且 `data!=null` 时，直接返回真实业务值。
 * 2. 否则抛出 `RemoteApiException`，由上层统一处理错误提示或兜底逻辑。
 *
 * 这样 repository 可以把“判定成功并取 data”的重复代码压缩到一个扩展函数里。
 *
 * @return 成功响应中的真实业务数据。
 * @throws RemoteApiException 当服务端返回失败或缺少必需 data 时抛出。
 */
fun <T> ApiResponseDto<T>.requireData(): T {
    if (success && data != null) return data
    throw RemoteApiException(message.ifBlank { code.ifBlank { "Remote api call failed." } })
}
