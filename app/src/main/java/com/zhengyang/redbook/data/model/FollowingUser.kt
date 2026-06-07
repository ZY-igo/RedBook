package com.zhengyang.redbook.data.model

data class FollowingUser(
    val id: String,
    val name: String,
    val subtitle: String,
    val avatarColorHex: String,
    val badge: String? = null
)
