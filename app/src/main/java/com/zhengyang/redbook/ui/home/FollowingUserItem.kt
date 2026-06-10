package com.zhengyang.redbook.ui.home

/**
 * 首页“关注”区用户模型。
 *
 * @property id 用户唯一标识。
 * @property name 用户显示名称。
 * @property subtitle 推荐原因或副标题。
 * @property avatarUrl 用户头像地址，为空时使用文字头像。
 * @property avatarColorHex 文字头像底色。
 * @property badge 展示在名字后的角标文案。
 */
data class FollowingUserItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val avatarUrl: String? = null,
    val avatarColorHex: String,
    val badge: String? = null
)
