package com.zhengyang.redbook.data.remote.interceptor

import com.zhengyang.redbook.data.auth.AuthRepositoryImpl
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AuthTokenInterceptor @Inject constructor(
    private val authRepository: AuthRepositoryImpl
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = authRepository.peekAccessToken()
        if (token.isNullOrBlank()) {
            return chain.proceed(chain.request())
        }
        val authenticatedRequest = chain.request().newBuilder()
            .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$token")
            .build()
        return chain.proceed(authenticatedRequest)
    }

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}
