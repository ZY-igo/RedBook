/**
 * 文件说明： DraftEntity.kt
 * 作用： 定义数据层使用的模型结构，用于持久化、映射转换或仓储内部的数据传递。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "draft")
data class DraftEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val mediaUrls: List<String>,
    val mediaType: String,
    val location: String?,
    val tags: List<String>,
    val savedAt: Long,
    val isAutoSaved: Boolean
)
