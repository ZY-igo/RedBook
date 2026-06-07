/**
 * 文件说明：InterestPersonEntity.kt
 * 作用：定义 Interest Person Entity 相关本地数据实体结构。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.model

/**
 * 感兴趣的人实体
 *
 * 用于保存“我的”页面中推荐关注的创作者数据，
 * 便于界面层按既定顺序渲染用户卡片。
 */
data class InterestPersonEntity(
    /** 用户唯一标识。 */
    val id: String,

    /** 头像占位文本。 */
    val avatarText: String,

    /** 用户展示名称。 */
    val name: String,

    /** 粉丝数或推荐说明文案。 */
    val fansText: String,

    /** 头像背景资源 ID。 */
    val avatarBackgroundRes: Int,

    /** 排序值，值越小越靠前。 */
    val sortOrder: Int
)
