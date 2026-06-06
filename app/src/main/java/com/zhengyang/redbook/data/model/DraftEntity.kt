package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "draft")
data class DraftEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val mediaUrls: List<String>,
    val mediaType: String,
    val location: String?,
    val tags: List<String>,
    val savedAt: Long,
    val isAutoSaved: Boolean
)
