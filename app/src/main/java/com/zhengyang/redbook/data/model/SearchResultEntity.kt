package com.zhengyang.redbook.data.model

import androidx.room.Entity

@Entity(
    tableName = "search_result",
    primaryKeys = ["keyword", "id"]
)
data class SearchResultEntity(
    val id: String,
    val keyword: String,
    val matchTokens: String,
    val filter: String,
    val title: String,
    val subtitle: String,
    val meta: String,
    val badge: String,
    val badgeColorResName: String,
    val sortOrder: Int
)
