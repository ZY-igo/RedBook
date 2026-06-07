/**
 * 文件说明：UserEntity.kt
 * 作用：定义 User Entity 相关本地数据实体结构。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.model

/**
 * 用户实体
 *
 * 用于承载用户资料页、笔记作者信息或关系判断所需的基础字段，
 * 避免不同业务场景重复定义相似结构。
 */
data class UserEntity(
    /** 用户唯一标识。 */
    val id: String,

    /** 用户昵称。 */
    val name: String,

    /** 头像地址，可为空。 */
    val avatarUrl: String?,

    /** 头像占位文本。 */
    val avatarText: String,

    /** 头像背景色值。 */
    val avatarColorHex: String,

    /** 个人简介，可为空。 */
    val bio: String?,

    /** 所在地，可为空。 */
    val location: String?,

    /** 生日信息，可为空。 */
    val birthday: String?,

    /** 性别信息，可为空。 */
    val gender: String?,

    /** 职业信息，可为空。 */
    val job: String?,

    /** 学校信息，可为空。 */
    val school: String?,

    /** 关注数。 */
    val followingCount: Int,

    /** 粉丝数。 */
    val fansCount: Int,

    /** 获赞与收藏数。 */
    val likesCount: Int,

    /** 发布笔记数。 */
    val noteCount: Int,

    /** 是否已认证。 */
    val isVerified: Boolean,

    /** 当前登录用户是否已关注该用户。 */
    val isFollowing: Boolean
)
