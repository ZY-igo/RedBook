/**
 * 文件说明：HomeCardItem.kt
 * 作用：定义首页卡片条目的界面展示模型。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

/**
 * 首页卡片 UI 模型
 *
 * 用于承载首页图文或视频卡片所需的展示字段，
 * 包括封面信息、作者信息、配色信息和骨架态标记。
 */
data class HomeCardItem(
    /** 卡片唯一标识。 */
    val id: String,

    /** 卡片标题。 */
    val title: String,

    /** 作者名称。 */
    val author: String,

    /** 点赞数展示文案。 */
    val likeCount: String,

    /** 卡片角标文案。 */
    val badge: String,

    /** 封面补充说明文案。 */
    val coverLabel: String,

    /** 封面建议高度，单位为 dp。 */
    val coverHeightDp: Int,

    /** 卡片媒体类型。 */
    val mediaType: MediaType = MediaType.IMAGE,

    /** 图片地址列表，供多图详情页使用。 */
    val imageUrls: List<String> = emptyList(),

    /** 首图地址。 */
    val imageUrl: String? = null,

    /** 视频地址。 */
    val videoUrl: String? = null,

    /** 视频封面地址。 */
    val videoCoverUrl: String? = null,

    /** 卡片背景渐变起始色。 */
    val startColorHex: String,

    /** 卡片背景渐变结束色。 */
    val endColorHex: String,

    /** 作者头像背景色。 */
    val avatarColorHex: String,

    /** 是否为骨架屏占位数据。 */
    val isSkeleton: Boolean = false
) {
    /**
     * 首页卡片媒体类型
     *
     * 用于区分当前卡片是图片内容还是视频内容，
     * 便于列表点击后跳转到对应详情页布局。
     */
    enum class MediaType {
        /** 图片内容。 */
        IMAGE,

        /** 视频内容。 */
        VIDEO
    }
}
