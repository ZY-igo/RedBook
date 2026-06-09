/**
 * 文件说明：FollowingUser.kt
 * 作用：定义首页关注推荐用户领域模型。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

/**
 * 关注推荐用户模型
 *
 * 用于在仓储层与界面映射层之间传递推荐关注用户信息，
 * 避免上层直接依赖本地实体或远程 DTO。
 */
data class FollowingUser(
    /** 用户唯一标识。 */
    val id: String,

    /** 用户昵称。 */
    val name: String,

    /** 推荐原因或描述文案。 */
    val subtitle: String,

    val avatarUrl: String? = null,

    /** 头像背景色值。 */
    val avatarColorHex: String,

    /** 展示角标文案，可为空。 */
    val badge: String? = null
)
