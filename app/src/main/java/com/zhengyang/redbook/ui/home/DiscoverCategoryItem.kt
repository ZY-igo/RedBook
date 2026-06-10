package com.zhengyang.redbook.ui.home

/**
 * 首页分类的 UI 模型。
 *
 * @property id 分类唯一标识。
 * @property title 分类在界面上的展示标题。
 * @property bucket 分类所属的业务分组。
 * @property usesWaterfall 当前分类是否使用双列瀑布流布局。
 * @property isDefaultSelected 当前分类是否是默认选中项。
 */
data class DiscoverCategoryItem(
    val id: String,
    val title: String,
    val bucket: DiscoverCategoryBucket,
    val usesWaterfall: Boolean,
    val isDefaultSelected: Boolean
)

/**
 * 首页分类的语义分组。
 */
enum class DiscoverCategoryBucket {
    /** 推荐类目。 */
    RECOMMEND,

    /** 红色主题或主推类目。 */
    RED,

    /** 直播类目。 */
    LIVE,

    /** 剧集类目。 */
    DRAMA,

    /** 技巧攻略类目。 */
    TIPS,

    /** 穿搭类目。 */
    OUTFIT,

    /** 美食类目。 */
    FOOD,

    /** 旅行类目。 */
    TRAVEL
}
