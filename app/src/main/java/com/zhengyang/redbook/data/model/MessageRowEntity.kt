/**
 * 文件说明： MessageRowEntity.kt
 * 作用： 定义数据层使用的模型结构，用于持久化、映射转换或仓储内部的数据传递。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_row")
data class MessageRowEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val subtitle: String,
    val timeText: String,
    val backgroundRes: Int,
    val iconRes: Int?,
    val avatarText: String?,
    val iconSizeDp: Int,
    val showsVerifiedBadge: Boolean,
    val showsRedDot: Boolean,
    val sortOrder: Int
)
