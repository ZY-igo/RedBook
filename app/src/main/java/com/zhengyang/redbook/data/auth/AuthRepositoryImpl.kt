package com.zhengyang.redbook.data.auth

import com.zhengyang.redbook.data.remote.AuthApiService
import com.zhengyang.redbook.data.remote.model.RemoteLogoutRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteMockLoginRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteRefreshTokenRequestDto
import com.zhengyang.redbook.data.remote.requireData
import com.zhengyang.redbook.data.remote.model.toDomain
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val sessionStorage: AuthSessionStorage
) : AuthRepository {

    private val refreshMutex = Mutex()

    @Volatile
    private var currentSession: AuthSession? = sessionStorage.loadSession()

    override fun currentSession(): AuthSession? = currentSession

    override fun accountHint(): SavedAccountHint = sessionStorage.loadAccountHint()

    override fun isLoggedIn(): Boolean = currentSession != null

    override suspend fun restoreSession(): Boolean = withContext(Dispatchers.IO) {
        val session = currentSession ?: sessionStorage.loadSession()?.also { currentSession = it } ?: return@withContext false
        if (session.refreshExpiresAt.isBefore(Instant.now())) {
            clearSession()
            return@withContext false
        }
        if (session.accessExpiresAt.isAfter(Instant.now())) {
            return@withContext true
        }
        refreshSession(session.accessToken) != null
    }

    override suspend fun login(request: AuthLoginRequest): AuthSession = withContext(Dispatchers.IO) {
        val normalizedUserId = request.userId.trim()
        val normalizedNickname = request.nickname.trim()
        val session = authApiService.mockLogin(
            RemoteMockLoginRequestDto(
                userId = normalizedUserId,
                nickname = normalizedNickname
            )
        ).requireData().toDomain()
        updateSession(session)
        sessionStorage.saveAccountHint(SavedAccountHint(normalizedUserId, normalizedNickname))
        session
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        val session = currentSession
        runCatching {
            authApiService.logout(
                authorization = session?.accessToken?.let { "Bearer $it" },
                request = RemoteLogoutRequestDto(session?.refreshToken)
            ).requireData()
        }
        clearSession()
    }

    fun peekAccessToken(): String? = currentSession?.accessToken

    fun refreshSessionBlocking(staleToken: String?): String? {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            refreshSession(staleToken)?.accessToken
        }
    }

    private suspend fun refreshSession(staleToken: String?): AuthSession? {
        return refreshMutex.withLock {
            val session = currentSession ?: sessionStorage.loadSession()?.also { currentSession = it } ?: return@withLock null
            if (session.refreshExpiresAt.isBefore(Instant.now())) {
                clearSession()
                return@withLock null
            }
            if (!staleToken.isNullOrBlank() && staleToken != session.accessToken) {
                return@withLock session
            }
            val refreshed = runCatching {
                authApiService.refreshToken(
                    RemoteRefreshTokenRequestDto(session.refreshToken)
                ).requireData().toDomain()
            }.getOrElse {
                clearSession()
                return@withLock null
            }
            updateSession(refreshed)
            refreshed
        }
    }

    private fun updateSession(session: AuthSession) {
        currentSession = session
        sessionStorage.saveSession(session)
    }

    private fun clearSession() {
        currentSession = null
        sessionStorage.clearSession()
    }
}
