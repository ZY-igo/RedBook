package com.zhengyang.redbook.ui.home

data class FollowingUserItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val avatarColorHex: String,
    val badge: String? = null
)
