/**
 * 文件说明： MyProfileEntity.kt
 * 作用： 定义数据层使用的模型结构，用于持久化、映射转换或仓储内部的数据传递。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "my_profile")
data class MyProfileEntity(
    @PrimaryKey
    val id: String = "self",
    val name: String,
    val avatarText: String,
    val avatarColorHex: String,
    val bio: String?,
    val followingCount: Int,
    val fansCount: Int,
    val likesCount: Int,
    val noteCount: Int
)
