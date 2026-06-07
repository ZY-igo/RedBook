package com.zhengyang.redbook.data.model

data class DiscoverCategory(
    val id: String,
    val title: String,
    val bucket: String,
    val usesWaterfall: Boolean,
    val isDefaultSelected: Boolean
)
