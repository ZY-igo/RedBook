package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_item")
data class NoteItem(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val likeCount: Int
)
