package com.zhengyang.redbook.data.auth

interface AuthRepository {
    fun currentSession(): AuthSession?

    fun accountHint(): SavedAccountHint

    fun isLoggedIn(): Boolean

    suspend fun restoreSession(): Boolean

    suspend fun login(request: AuthLoginRequest): AuthSession

    suspend fun logout()
}
