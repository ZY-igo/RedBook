/**
 * 文件说明： NoteItem.kt
 * 作用： 定义数据层使用的模型结构，用于持久化、映射转换或仓储内部的数据传递。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_item")
data class NoteItem(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val likeCount: Int,
    val author: String,
    val mediaType: String,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val coverUrl: String? = null,
    val coverHeightDp: Int = 220
)
