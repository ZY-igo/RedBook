/**
 * 文件说明：UserNoteEntity.kt
 * 作用：定义 User Note Entity 相关本地数据实体结构。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.model

/**
 * 用户笔记关联实体
 *
 * 用于表示某个用户发布或持有的一条笔记记录，
 * 便于个人主页、草稿列表或作品列表直接渲染。
 */
data class UserNoteEntity(
    /** 笔记唯一标识。 */
    val id: String,

    /** 笔记所属用户 ID。 */
    val userId: String,

    /** 笔记标题。 */
    val title: String,

    /** 笔记描述摘要。 */
    val description: String,

    /** 笔记封面地址。 */
    val coverUrl: String,

    /** 笔记媒体类型。 */
    val mediaType: String,

    /** 点赞数。 */
    val likeCount: Int,

    /** 评论数。 */
    val commentCount: Int,

    /** 分享数。 */
    val shareCount: Int,

    /** 创建时间戳，单位为毫秒。 */
    val createdAt: Long,

    /** 是否为草稿记录。 */
    val isDraft: Boolean
)
