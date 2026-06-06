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
