package com.zhengyang.redbook.ui.home

data class HomeCardItem(
    val id: String,
    val title: String,
    val author: String,
    val likeCount: String,
    val badge: String,
    val coverLabel: String,
    val coverHeightDp: Int,
    val startColorHex: String,
    val endColorHex: String,
    val avatarColorHex: String
)
