/**
 * 文件说明：CollectionEntity.kt
 * 作用：定义收藏表对应的本地实体结构。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 收藏记录实体
 *
 * 用于在 Room 中保存用户收藏过的笔记快照，
 * 以便收藏页或离线场景直接读取所需基础字段。
 */
@Entity(tableName = "collection")
data class CollectionEntity(
    /** 收藏记录唯一标识。 */
    @PrimaryKey
    val id: String,

    /** 发起收藏的用户 ID。 */
    val userId: String,

    /** 被收藏的笔记 ID。 */
    val noteId: String,

    /** 收藏时记录的笔记标题快照。 */
    val noteTitle: String,

    /** 收藏时记录的笔记封面地址。 */
    val noteCoverUrl: String,

    /** 收藏时记录的笔记作者名称。 */
    val noteAuthor: String,

    /** 收藏时间戳，单位为毫秒。 */
    val collectedAt: Long
)
