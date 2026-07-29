package com.zhengyang.redbook.data.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSessionStorage @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val preferences by lazy(LazyThreadSafetyMode.NONE) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun loadSession(): AuthSession? {
        val userId = preferences.getString(KEY_USER_ID, null) ?: return null
        val nickname = preferences.getString(KEY_NICKNAME, null) ?: return null
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val accessExpiresAtMillis = preferences.getLong(KEY_ACCESS_EXPIRES_AT, 0L)
        val refreshExpiresAtMillis = preferences.getLong(KEY_REFRESH_EXPIRES_AT, 0L)
        if (accessExpiresAtMillis <= 0L || refreshExpiresAtMillis <= 0L) return null
        return AuthSession(
            userId = userId,
            nickname = nickname,
            accessToken = accessToken,
            accessExpiresAt = Instant.ofEpochMilli(accessExpiresAtMillis),
            refreshToken = refreshToken,
            refreshExpiresAt = Instant.ofEpochMilli(refreshExpiresAtMillis)
        )
    }

    fun saveSession(session: AuthSession) {
        preferences.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_NICKNAME, session.nickname)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putLong(KEY_ACCESS_EXPIRES_AT, session.accessExpiresAt.toEpochMilli())
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_REFRESH_EXPIRES_AT, session.refreshExpiresAt.toEpochMilli())
            .putString(KEY_LAST_USER_ID, session.userId)
            .putString(KEY_LAST_NICKNAME, session.nickname)
            .apply()
    }

    fun clearSession() {
        preferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_NICKNAME)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_ACCESS_EXPIRES_AT)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_REFRESH_EXPIRES_AT)
            .apply()
    }

    fun saveAccountHint(hint: SavedAccountHint) {
        preferences.edit()
            .putString(KEY_LAST_USER_ID, hint.userId)
            .putString(KEY_LAST_NICKNAME, hint.nickname)
            .apply()
    }

    fun loadAccountHint(): SavedAccountHint {
        return SavedAccountHint(
            userId = preferences.getString(KEY_LAST_USER_ID, null).orEmpty(),
            nickname = preferences.getString(KEY_LAST_NICKNAME, null).orEmpty()
        )
    }

    companion object {
        private const val PREFS_NAME = "auth_session_storage"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_ACCESS_EXPIRES_AT = "access_expires_at"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_REFRESH_EXPIRES_AT = "refresh_expires_at"
        private const val KEY_LAST_USER_ID = "last_user_id"
        private const val KEY_LAST_NICKNAME = "last_nickname"
    }
}
