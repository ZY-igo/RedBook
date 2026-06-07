/**
 * 文件说明：SearchGuessEntity.kt
 * 作用：定义 Search Guess Entity 相关本地数据实体结构。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.model

/**
 * 猜你想搜实体
 *
 * 用于保存搜索页初始化时展示的推荐关键词，
 * 支持按固定顺序读取并直接映射为界面模型。
 */
data class SearchGuessEntity(
    /** 推荐词唯一标识。 */
    val id: String,

    /** 推荐词标题。 */
    val title: String,

    /** 推荐词补充说明。 */
    val meta: String,

    /** 排序值，值越小越靠前。 */
    val sortOrder: Int
)
