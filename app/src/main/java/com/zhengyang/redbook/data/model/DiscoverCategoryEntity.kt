/**
 * 文件说明： DiscoverCategoryEntity.kt
 * 作用： 定义数据层使用的模型结构，用于持久化、映射转换或仓储内部的数据传递。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discover_category")
data class DiscoverCategoryEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val bucket: String,
    val usesWaterfall: Boolean,
    val sortOrder: Int,
    val isDefaultSelected: Boolean
)
