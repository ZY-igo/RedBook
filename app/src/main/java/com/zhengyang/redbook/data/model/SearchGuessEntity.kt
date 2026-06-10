package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_guess")
data class SearchGuessEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val meta: String,
    val sortOrder: Int
)
