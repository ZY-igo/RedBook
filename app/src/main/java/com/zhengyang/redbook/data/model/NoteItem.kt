/**
 * 文件说明：NoteItem.kt
 * 作用：定义笔记主表对应的本地实体结构。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 笔记实体
 *
 * 用于在本地数据库中保存笔记基础信息，
 * 兼顾列表展示、详情跳转和离线缓存等场景。
 */
@Entity(tableName = "note_item")
data class NoteItem(
    /** 笔记唯一标识。 */
    @PrimaryKey
    val id: String,

    /** 笔记标题。 */
    val title: String,

    /** 笔记正文摘要或描述。 */
    val description: String,

    /** 点赞数。 */
    val likeCount: Int,

    /** 作者名称。 */
    val author: String,

    /** 媒体类型，通常对应图文或视频。 */
    val mediaType: String,

    /** 首图地址，可为空。 */
    val imageUrl: String? = null,

    /** 视频地址，可为空。 */
    val videoUrl: String? = null,

    /** 封面地址，可为空。 */
    val coverUrl: String? = null,

    /** 封面建议高度，默认 220dp。 */
    val coverHeightDp: Int = 220
)
