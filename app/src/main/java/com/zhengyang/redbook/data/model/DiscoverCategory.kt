/**
 * 文件说明：DiscoverCategory.kt
 * 作用：定义首页分类领域模型。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

/**
 * 首页分类领域模型
 *
 * 用于在仓储层与用例层之间传递首页分类信息，
 * 避免上层直接依赖本地实体或远端 DTO 结构。
 */
data class DiscoverCategory(
    /** 分类唯一标识。 */
    val id: String,
    /** 分类展示标题。 */
    val title: String,
    /** 分类所属业务桶字符串。 */
    val bucket: String,
    /** 当前分类是否使用瀑布流布局。 */
    val usesWaterfall: Boolean,
    /** 是否为默认选中的分类。 */
    val isDefaultSelected: Boolean
)
