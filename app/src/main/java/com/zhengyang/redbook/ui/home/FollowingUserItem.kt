/**
 * 文件说明：FollowingUserItem.kt
 * 作用：定义首页推荐关注用户的界面展示模型。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

/**
 * 推荐关注用户 UI 模型
 *
 * 用于在首页关注流顶部展示推荐用户卡片，
 * 承载昵称、说明文案、头像配色和角标等信息。
 */
data class FollowingUserItem(
    /** 用户唯一标识。 */
    val id: String,

    /** 用户昵称。 */
    val name: String,

    /** 推荐原因或补充说明文案。 */
    val subtitle: String,

    /** 头像背景色值。 */
    val avatarColorHex: String,

    /** 角标文案，可为空。 */
    val badge: String? = null
)
