/**
 * 文件说明：PersonSuggestionEntity.kt
 * 作用：定义 Person Suggestion Entity 相关本地数据实体结构。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.model

/**
 * 推荐用户卡片实体
 *
 * 用于承载消息页或其他推荐区域中的用户基础信息和展示顺序，
 * 便于列表层直接读取并渲染卡片内容。
 */
data class PersonSuggestionEntity(
    /** 推荐用户唯一标识。 */
    val id: String,

    /** 头像占位文本，通常取用户名首字或简称。 */
    val avatarText: String,

    /** 推荐用户展示名称。 */
    val name: String,

    /** 推荐原因或补充说明文案。 */
    val subtitle: String,

    /** 头像背景资源 ID。 */
    val avatarBackgroundRes: Int,

    /** 在推荐列表中的排序值，值越小越靠前。 */
    val sortOrder: Int
)
