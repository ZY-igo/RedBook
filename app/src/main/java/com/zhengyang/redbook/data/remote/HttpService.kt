package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.data.model.NoteItem
import okhttp3.OkHttpClient
import okhttp3.Request

class HttpService(
    private val okHttpClient: OkHttpClient = defaultClient()
) : ListContentApiService {
    override suspend fun getListContent(): List<NoteItem> {
        return executeGetListContent(
            Request.Builder()
                .url(BASE_URL)
                .get()
                .build()
        )
    }

    private fun executeGetListContent(request: Request): List<NoteItem> {
        return runCatching<List<NoteItem>> {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful || response.body == null) {
                    emptyList<NoteItem>()
                } else {
                    emptyList<NoteItem>()
                }
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val BASE_URL = "https://api.github.com/"

        private fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .build()
    }
}
