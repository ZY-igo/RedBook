/**
 * 文件说明：MessageRowEntity.kt
 * 作用：定义消息入口表对应的本地实体结构。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 消息入口实体
 *
 * 用于缓存消息页中每一行入口的文案、图标、状态和排序信息，
 * 便于页面在弱网或离线场景下直接读取。
 */
@Entity(tableName = "message_row")
data class MessageRowEntity(
    /** 入口唯一标识。 */
    @PrimaryKey
    val id: String,

    /** 主标题文案。 */
    val title: String,

    /** 副标题文案。 */
    val subtitle: String,

    /** 右侧时间展示文案。 */
    val timeText: String,

    /** 背景资源 ID。 */
    val backgroundRes: Int,

    /** 图标资源 ID，允许为空。 */
    val iconRes: Int?,

    /** 头像占位文本，通常在无图片头像时使用。 */
    val avatarText: String?,

    /** 图标显示尺寸，单位为 dp。 */
    val iconSizeDp: Int,

    /** 是否显示认证标识。 */
    val showsVerifiedBadge: Boolean,

    /** 是否显示红点提示。 */
    val showsRedDot: Boolean,

    /** 列表排序值，值越小越靠前。 */
    val sortOrder: Int
)
