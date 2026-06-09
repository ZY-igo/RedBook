/**
 * 文件说明：HomeDiscoverItem.kt
 * 作用：定义首页发现流条目领域模型。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.model

/**
 * 首页发现流条目领域模型
 *
 * 用于承载首页发现流卡片所需的业务字段，
 * 供用例层映射为界面模型或在不同首页分区之间复用。
 */
data class HomeDiscoverItem(
    /** 条目唯一标识。 */
    val id: String,
    /** 条目标题。 */
    val title: String,
    /** 作者名称。 */
    val author: String,
    val avatarUrl: String? = null,
    /** 点赞数展示文案。 */
    val likeCount: String,
    /** 角标文案。 */
    val badge: String,
    /** 封面附加说明文案。 */
    val coverLabel: String,
    /** 封面建议高度，单位为 dp。 */
    val coverHeightDp: Int,
    /** 内容媒体类型。 */
    val mediaType: MediaType,
    /** 图片地址集合，主要用于图文场景。 */
    val imageUrls: List<String> = emptyList(),
    /** 首图地址。 */
    val imageUrl: String? = null,
    /** 视频播放地址。 */
    val videoUrl: String? = null,
    /** 视频封面地址。 */
    val videoCoverUrl: String? = null,
    /** 背景渐变起始色。 */
    val startColorHex: String,
    /** 背景渐变结束色。 */
    val endColorHex: String,
    /** 作者头像背景色。 */
    val avatarColorHex: String
)
