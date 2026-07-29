package com.zhengyang.redbook.data.auth

import java.time.Instant

data class AuthSession(
    val userId: String,
    val nickname: String,
    val accessToken: String,
    val accessExpiresAt: Instant,
    val refreshToken: String,
    val refreshExpiresAt: Instant
)

data class AuthLoginRequest(
    val userId: String,
    val nickname: String
)

data class SavedAccountHint(
    val userId: String = "",
    val nickname: String = ""
)
