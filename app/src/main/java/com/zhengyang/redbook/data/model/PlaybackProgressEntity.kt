/**
 * 文件说明：PlaybackProgressEntity.kt
 * 作用：定义播放进度表对应的本地实体结构。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 播放进度实体
 *
 * 用于记录视频笔记的播放位置、总时长与倍速，
 * 以支持用户下次进入详情页时继续播放。
 */
@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    /** 笔记 ID，同时作为进度记录主键。 */
    @PrimaryKey
    val noteId: String,

    /** 当前播放位置，单位为毫秒。 */
    val positionMs: Long,

    /** 视频总时长，单位为毫秒。 */
    val durationMs: Long,

    /** 最近一次保存时的播放倍速。 */
    val playbackSpeed: Float,

    /** 最近播放时间戳，单位为毫秒。 */
    val lastPlayedAt: Long
)
