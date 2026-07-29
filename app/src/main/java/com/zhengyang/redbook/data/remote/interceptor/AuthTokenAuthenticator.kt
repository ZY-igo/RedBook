package com.zhengyang.redbook.data.remote.interceptor

import com.zhengyang.redbook.data.auth.AuthRepositoryImpl
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

@Singleton
class AuthTokenAuthenticator @Inject constructor(
    private val authRepository: AuthRepositoryImpl
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        val staleHeader = response.request.header(HEADER_AUTHORIZATION)
        val staleToken = staleHeader?.removePrefix(BEARER_PREFIX)?.trim()
        val refreshedToken = authRepository.refreshSessionBlocking(staleToken) ?: return null
        return response.request.newBuilder()
            .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$refreshedToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}
