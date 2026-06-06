package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discover_category")
data class DiscoverCategoryEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val bucket: String,
    val usesWaterfall: Boolean,
    val sortOrder: Int,
    val isDefaultSelected: Boolean
)
