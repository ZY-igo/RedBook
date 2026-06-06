/**
 * 文件说明：SearchHistoryEntity.kt
 * 作用：定义搜索历史数据模型，用于存储用户搜索过的关键字及最近使用时间。
 * 备注：该实体通常被搜索页的历史记录列表、去重逻辑和最近搜索排序逻辑共同使用。
 */
package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 表示一条本地搜索历史记录。
 *
 * 该实体直接以搜索词作为主键，因此同一个关键字重复搜索时，
 * 只会更新已有记录，而不会在本地表中生成重复数据。
 */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    /**
     * 用户实际输入并提交过的搜索词。
     *
     * 该字段同时作为表主键使用，用来保证历史记录按关键字维度唯一。
     */
    @PrimaryKey
    val query: String,

    /**
     * 最近一次使用该搜索词的时间戳。
     *
     * 该值通常用于按“最近搜索”排序，也便于后续按时间清理旧记录。
     */
    val updatedAt: Long
)
