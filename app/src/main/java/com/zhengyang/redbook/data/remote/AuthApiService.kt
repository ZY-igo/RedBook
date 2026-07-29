package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.data.remote.model.ApiResponseDto
import com.zhengyang.redbook.data.remote.model.RemoteAuthSessionDto
import com.zhengyang.redbook.data.remote.model.RemoteLogoutRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteMockLoginRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteRefreshTokenRequestDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/v1/auth/mock-login")
    suspend fun mockLogin(
        @Body request: RemoteMockLoginRequestDto
    ): ApiResponseDto<RemoteAuthSessionDto>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: RemoteRefreshTokenRequestDto
    ): ApiResponseDto<RemoteAuthSessionDto>

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String? = null,
        @Body request: RemoteLogoutRequestDto = RemoteLogoutRequestDto(null)
    ): ApiResponseDto<Map<String, Any?>>
}
