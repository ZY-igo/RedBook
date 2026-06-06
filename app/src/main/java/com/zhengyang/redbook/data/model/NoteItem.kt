package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_item")
data class NoteItem(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val likeCount: Int,
    val author: String,
    val mediaType: String,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val coverUrl: String? = null,
    val coverHeightDp: Int = 220
)
