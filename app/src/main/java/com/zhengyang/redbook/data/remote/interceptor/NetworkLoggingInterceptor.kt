/**
 * 文件说明：NetworkLoggingInterceptor.kt
 * 作用：记录网络请求的关键日志信息，用于开发调试与问题定位。
 * 备注：该拦截器仅负责观测，不修改请求内容和响应结果。
 */
package com.zhengyang.redbook.data.remote.interceptor

import com.zhengyang.redbook.utils.AppLogger
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 网络日志拦截器。
 * 
 * 当前记录的信息包括：
 * 1. 请求方法与 URL。
 * 2. 响应状态码。
 * 3. 整体请求耗时。
 * 4. 请求失败时的异常堆栈。
 * 
 * 它主要用于开发调试、接口联调与问题定位。
 */
class NetworkLoggingInterceptor : Interceptor {

    /**
     * 拦截网络请求，在请求前后打印日志并统计耗时。
     * 
     * @param chain OkHttp 的拦截器链对象，用于传递请求和获取响应。
     * @return 返回处理后的响应对象，不修改任何内容。
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        // 获取当前请求对象
        val request = chain.request()
        
        // 记录请求开始时间（纳秒级，保证精度）
        val startNs = System.nanoTime()
        
        // 打印请求日志：方法 + URL
        AppLogger.d("Network", "-> ${request.method} ${request.url}")
        
        // 使用 try-catch 包裹，捕获请求过程中的异常
        return try {
            // 执行请求，传递到下一个拦截器或最终发起网络请求
            val response = chain.proceed(request)
            
            // 计算请求耗时：(结束时间 - 开始时间) / 1_000_000 转换为毫秒
            val durationMs = (System.nanoTime() - startNs) / 1_000_000
            
            // 打印成功响应日志：状态码 + 方法 + URL + 耗时
            AppLogger.d(
                "Network",
                "<- ${response.code} ${request.method} ${request.url} (${durationMs}ms)"
            )
            
            // 返回响应，不做任何修改
            response
            
        } catch (throwable: Throwable) {
            // 请求失败时，同样计算耗时
            val durationMs = (System.nanoTime() - startNs) / 1_000_000
            
            // 打印失败日志：标记 failed + 方法 + URL + 耗时 + 异常堆栈
            AppLogger.e(
                "Network",
                "<- failed ${request.method} ${request.url} (${durationMs}ms)",
                throwable
            )
            
            // 重新抛出异常，让上层处理
            throw throwable
        }
    }
}