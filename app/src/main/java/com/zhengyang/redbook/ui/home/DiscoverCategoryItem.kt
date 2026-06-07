/**
 * 文件说明：DiscoverCategoryItem.kt
 * 作用：定义首页分类的界面展示模型。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

/**
 * 首页分类 UI 模型
 *
 * 用于承载首页分类栏展示和切换所需的数据，
 * 包括分类标题、业务桶和布局配置。
 */
data class DiscoverCategoryItem(
    /** 分类唯一标识。 */
    val id: String,

    /** 分类展示标题。 */
    val title: String,

    /** 分类所属业务桶。 */
    val bucket: DiscoverCategoryBucket,

    /** 当前分类是否使用瀑布流布局。 */
    val usesWaterfall: Boolean,

    /** 是否为默认选中的分类。 */
    val isDefaultSelected: Boolean
)

/**
 * 首页分类业务桶枚举
 *
 * 用于对首页分类做语义分组，
 * 便于界面和业务层做定制化处理。
 */
enum class DiscoverCategoryBucket {
    /** 推荐分类。 */
    RECOMMEND,

    /** 红色主题分类。 */
    RED,

    /** 直播分类。 */
    LIVE,

    /** 剧集分类。 */
    DRAMA,

    /** 攻略技巧分类。 */
    TIPS,

    /** 穿搭分类。 */
    OUTFIT,

    /** 美食分类。 */
    FOOD,

    /** 旅行分类。 */
    TRAVEL
}
