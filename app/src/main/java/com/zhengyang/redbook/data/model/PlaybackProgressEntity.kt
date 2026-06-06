package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey
    val noteId: String,
    val positionMs: Long,
    val durationMs: Long,
    val playbackSpeed: Float,
    val lastPlayedAt: Long
)
