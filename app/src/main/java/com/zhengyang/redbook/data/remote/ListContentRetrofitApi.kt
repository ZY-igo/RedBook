package com.zhengyang.redbook.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ListContentRetrofitApi {

    @GET("{path}")
    suspend fun getListContent(
        @Path(value = "path", encoded = true) path: String
    ): Response<ResponseBody>
}
