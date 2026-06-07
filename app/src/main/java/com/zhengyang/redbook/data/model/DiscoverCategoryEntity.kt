/**
 * 文件说明：DiscoverCategoryEntity.kt
 * 作用：定义发现页分类表对应的本地实体结构。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 发现页分类实体
 *
 * 用于在本地数据库中缓存发现页分类列表，
 * 并保留排序信息与默认选中状态。
 */
@Entity(tableName = "discover_category")
data class DiscoverCategoryEntity(
    /** 分类唯一标识。 */
    @PrimaryKey
    val id: String,

    /** 分类展示标题。 */
    val title: String,

    /** 分类所属业务桶，用于界面或接口分流。 */
    val bucket: String,

    /** 是否使用瀑布流布局。 */
    val usesWaterfall: Boolean,

    /** 分类排序值，值越小越靠前。 */
    val sortOrder: Int,

    /** 是否为默认选中的分类。 */
    val isDefaultSelected: Boolean
)
