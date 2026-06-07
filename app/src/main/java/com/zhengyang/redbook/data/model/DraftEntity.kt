/**
 * 文件说明：DraftEntity.kt
 * 作用：定义发布草稿表对应的本地实体结构。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 草稿实体
 *
 * 用于保存发布流程中的临时内容，
 * 支持自动保存、手动保存和草稿恢复。
 */
@Entity(tableName = "draft")
data class DraftEntity(
    /** 草稿唯一标识，建议使用 UUID 形式保证不同草稿不冲突。 */
    @PrimaryKey
    val id: String,

    /** 草稿标题。 */
    val title: String,

    /** 草稿正文描述。 */
    val description: String,

    /** 媒体资源地址列表。 */
    val mediaUrls: List<String>,

    /** 媒体类型。 */
    val mediaType: String,

    /** 发布地点，可为空。 */
    val location: String?,

    /** 标签列表。 */
    val tags: List<String>,

    /** 最近保存时间戳，单位为毫秒。 */
    val savedAt: Long,

    /** 是否为自动保存生成的草稿。 */
    val isAutoSaved: Boolean
)
