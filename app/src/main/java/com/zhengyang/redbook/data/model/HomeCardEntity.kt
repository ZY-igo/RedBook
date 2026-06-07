/**
 * 文件说明：HomeCardEntity.kt
 * 作用：定义首页卡片表对应的本地实体结构。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 首页卡片实体
 *
 * 用于缓存首页图文或视频卡片的展示字段，
 * 支持按分区和排序快速读取本地数据。
 */
@Entity(tableName = "home_card")
data class HomeCardEntity(
    /** 卡片唯一标识。 */
    @PrimaryKey
    val id: String,

    /** 所属内容分区标识，例如推荐、关注。 */
    val sectionKey: String,

    /** 卡片标题。 */
    val title: String,

    /** 作者名称。 */
    val author: String,

    /** 点赞数展示文案。 */
    val likeCount: String,

    /** 角标文案。 */
    val badge: String,

    /** 封面附加描述。 */
    val coverLabel: String,

    /** 封面建议高度，单位为 dp。 */
    val coverHeightDp: Int,

    /** 媒体类型，通常与 [MediaType] 对应。 */
    val mediaType: String,

    /** 图片地址。 */
    val imageUrl: String?,

    /** 视频地址。 */
    val videoUrl: String?,

    /** 视频封面地址。 */
    val videoCoverUrl: String?,

    /** 渐变起始色。 */
    val startColorHex: String,

    /** 渐变结束色。 */
    val endColorHex: String,

    /** 作者头像背景色。 */
    val avatarColorHex: String,

    /** 卡片排序值，值越小越靠前。 */
    val sortOrder: Int
)
