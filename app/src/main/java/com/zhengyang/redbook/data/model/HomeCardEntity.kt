/**
 * 文件说明： HomeCardEntity.kt
 * 作用： 定义数据层使用的模型结构，用于持久化、映射转换或仓储内部的数据传递。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_card")
data class HomeCardEntity(
    @PrimaryKey
    val id: String,
    val sectionKey: String,
    val title: String,
    val author: String,
    val likeCount: String,
    val badge: String,
    val coverLabel: String,
    val coverHeightDp: Int,
    val mediaType: String,
    val imageUrl: String?,
    val videoUrl: String?,
    val videoCoverUrl: String?,
    val startColorHex: String,
    val endColorHex: String,
    val avatarColorHex: String,
    val sortOrder: Int
)
