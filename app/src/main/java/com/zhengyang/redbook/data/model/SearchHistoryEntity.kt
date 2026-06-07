/**
 * 文件说明：SearchHistoryEntity.kt
 * 作用：定义搜索历史表对应的本地实体结构。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 搜索历史实体
 *
 * 以搜索词作为主键保存最近使用记录，
 * 从而在重复搜索同一关键词时执行覆盖更新而不是插入重复数据。
 */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    /** 用户实际提交过的搜索关键词，同时作为主键去重。 */
    @PrimaryKey
    val query: String,

    /** 最近一次使用该关键词的时间戳，单位为毫秒。 */
    val updatedAt: Long
)
