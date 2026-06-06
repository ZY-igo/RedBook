package com.zhengyang.redbook.data.model

data class UserEntity(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val avatarText: String,
    val avatarColorHex: String,
    val bio: String?,
    val location: String?,
    val birthday: String?,
    val gender: String?,
    val job: String?,
    val school: String?,
    val followingCount: Int,
    val fansCount: Int,
    val likesCount: Int,
    val noteCount: Int,
    val isVerified: Boolean,
    val isFollowing: Boolean
)
