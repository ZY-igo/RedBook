/**
 * 文件说明：RemoteResult.kt
 * 作用：定义 remote 层调用结果的统一封装模型。
 * 备注：用于在不依赖异常流的场景下，显式区分成功、HTTP 失败、网络失败和解析失败。
 */
package com.zhengyang.redbook.data.remote

/**
 * remote 层统一结果模型。
 *
 * 与直接抛异常相比，这种显式结果更适合需要精细分流的调用场景，
 * 例如 repository 希望对 HTTP 错误、网络错误和解析错误做不同处理时。
 */
sealed interface RemoteResult<out T> {

    /** 远端调用成功，并携带最终解析后的业务值。 */
    data class Success<T>(val value: T) : RemoteResult<T>

    /** 服务端返回了 HTTP 层错误状态码，同时保留原始响应体用于排查。 */
    data class HttpError(
        val code: Int,
        val body: String
    ) : RemoteResult<Nothing>

    /** 请求在网络链路层失败，例如断网、超时或连接异常。 */
    data class NetworkError(val cause: Throwable) : RemoteResult<Nothing>

    /** 响应成功返回，但 DTO 解析或二次转换业务模型时失败。 */
    data class ParseError(val cause: Throwable) : RemoteResult<Nothing>
}
