package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.data.remote.model.RemoteNoteDto
import com.zhengyang.redbook.utils.AppLogger
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HttpService @Inject constructor(
    private val retrofitApi: ListContentRetrofitApi,
    private val remoteApiConfig: RemoteApiConfig,
    private val responseParser: ListContentResponseParser,
    private val mockFactory: RemoteNoteMockFactory
) : ListContentApiService {

    override suspend fun getListContent(): RemoteResult<List<RemoteNoteDto>> = withContext(Dispatchers.IO) {
        if (remoteApiConfig.shouldUseMockData()) {
            return@withContext RemoteResult.Success(mockFactory.create())
        }
        executeGetListContent()
    }

    private suspend fun executeGetListContent(): RemoteResult<List<RemoteNoteDto>> {
        return try {
            val response = retrofitApi.getListContent(remoteApiConfig.listContentPath)
            val responseBody = response.body()?.string().orEmpty()
            val errorBody = response.errorBody()?.string().orEmpty()

            if (!response.isSuccessful) {
                return RemoteResult.HttpError(
                    code = response.code(),
                    body = errorBody.ifBlank { responseBody }
                )
            }

            AppLogger.d("HttpService", "Parsed list content response via Retrofit.")
            runCatching { responseParser.parse(responseBody) }
                .fold(
                    onSuccess = { RemoteResult.Success(it) },
                    onFailure = { RemoteResult.ParseError(it) }
                )
        } catch (error: IOException) {
            RemoteResult.NetworkError(error)
        }
    }
}
