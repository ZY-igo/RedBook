package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_card")
data class HomeCardEntity(
    @PrimaryKey
    val id: String,
    val sectionKey: String,
    val title: String,
    val author: String,
    val likeCount: String,
    val badge: String,
    val coverLabel: String,
    val coverHeightDp: Int,
    val mediaType: String,
    val imageUrl: String?,
    val videoUrl: String?,
    val videoCoverUrl: String?,
    val startColorHex: String,
    val endColorHex: String,
    val avatarColorHex: String,
    val sortOrder: Int
)
