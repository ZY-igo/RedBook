/**
 * 文件说明：SearchResultEntity.kt
 * 作用：定义 Search Result Entity 相关本地数据实体结构。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.model

/**
 * 搜索结果实体
 *
 * 用于承载搜索结果页中单条结果的标题、筛选类型和角标样式，
 * 便于仓储层、本地缓存或映射层统一处理。
 */
data class SearchResultEntity(
    /** 结果项唯一标识。 */
    val id: String,

    /** 触发该结果的原始搜索关键词。 */
    val keyword: String,

    /** 用于匹配高亮或检索的关键字集合字符串。 */
    val matchTokens: String,

    /** 结果所属筛选类型。 */
    val filter: String,

    /** 结果标题。 */
    val title: String,

    /** 结果副标题。 */
    val subtitle: String,

    /** 结果补充说明文案。 */
    val meta: String,

    /** 结果角标文案。 */
    val badge: String,

    /** 角标颜色资源名称。 */
    val badgeColorResName: String,

    /** 排序值，值越小越靠前。 */
    val sortOrder: Int
)
