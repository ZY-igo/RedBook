/**
 * 文件说明：MyProfileEntity.kt
 * 作用：定义个人资料表对应的本地实体结构。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 个人资料实体
 *
 * 用于保存当前登录用户的基础资料与统计信息，
 * 供个人页和编辑资料流程读取。
 */
@Entity(tableName = "my_profile")
data class MyProfileEntity(
    /** 固定主键，默认使用 self 表示当前用户。 */
    @PrimaryKey
    val id: String = "self",

    /** 用户昵称。 */
    val name: String,

    /** 头像占位文本。 */
    val avatarText: String,

    /** 头像背景色。 */
    val avatarColorHex: String,

    /** 个人简介，可为空。 */
    val bio: String?,

    /** 关注数。 */
    val followingCount: Int,

    /** 粉丝数。 */
    val fansCount: Int,

    /** 获赞与收藏总数。 */
    val likesCount: Int,

    /** 发布笔记数。 */
    val noteCount: Int
)
