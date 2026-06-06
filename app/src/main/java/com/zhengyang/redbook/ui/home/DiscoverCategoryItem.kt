/**
 * 文件说明： DiscoverCategoryItem.kt
 * 作用： 承载首页相关的界面状态与交互逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

data class DiscoverCategoryItem(
    val id: String,
    val title: String,
    val bucket: DiscoverCategoryBucket,
    val usesWaterfall: Boolean,
    val isDefaultSelected: Boolean
)

enum class DiscoverCategoryBucket {
    RECOMMEND,
    RED,
    LIVE,
    DRAMA,
    TIPS,
    OUTFIT,
    FOOD,
    TRAVEL
}
