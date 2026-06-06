/**
 * 文件说明： HttpService.kt
 * 作用： 封装远程数据访问相关逻辑，包括接口配置、请求行为和响应解析。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.data.remote.model.RemoteNoteDto
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class HttpService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val remoteApiConfig: RemoteApiConfig,
    private val responseParser: ListContentResponseParser,
    private val mockFactory: RemoteNoteMockFactory
) : ListContentApiService {

    override suspend fun getListContent(): List<RemoteNoteDto> = withContext(Dispatchers.IO) {
        if (remoteApiConfig.shouldUseMockData()) {
            return@withContext mockFactory.create()
        }
        executeGetListContent(buildListContentRequest())
    }

    private fun buildListContentRequest(): Request {
        val requestBuilder = Request.Builder()
            .url(remoteApiConfig.baseUrl.newBuilder().addPathSegments(remoteApiConfig.listContentPath).build())
            .get()

        remoteApiConfig.defaultHeaders.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        return requestBuilder.build()
    }

    private fun executeGetListContent(request: Request): List<RemoteNoteDto> {
        return runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) return@use mockFactory.create()

                responseParser.parse(responseBody).ifEmpty { mockFactory.create() }
            }
        }.getOrElse { mockFactory.create() }
    }
}
