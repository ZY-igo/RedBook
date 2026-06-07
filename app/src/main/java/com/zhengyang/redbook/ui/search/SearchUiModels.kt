/**
 * 文件说明：SearchUiModels.kt
 * 作用：定义搜索页面使用的界面状态、列表项模型与枚举类型。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.search

/**
 * 搜索页面总状态模型
 *
 * 用于承载搜索首页、联想建议页和搜索结果页共享的数据。
 * [SearchViewModel] 会持续产出该模型，[SearchActivity] 按状态内容决定界面展示。
 */
data class SearchUiState(
    /** 搜索历史文案列表，按展示顺序排列。 */
    val historyItems: List<String> = emptyList(),

    /** 默认态下展示的猜你想搜条目。 */
    val guessItems: List<SearchGuessItem> = emptyList(),

    /** 输入搜索词后返回的联想建议条目。 */
    val suggestionItems: List<SearchGuessItem> = emptyList(),

    /** 当前搜索词对应的结果条目集合。 */
    val resultItems: List<SearchResultItem> = emptyList(),

    /** 当前生效的搜索关键词。 */
    val currentQuery: String = "",

    /** 结果页当前选中的筛选条件。 */
    val currentFilter: ResultFilter = ResultFilter.ALL,

    /** 当前页面所处展示模式。 */
    val screenMode: SearchScreenMode = SearchScreenMode.DEFAULT
)

/**
 * 搜索联想或猜你想搜条目模型
 *
 * 同时用于默认态推荐和输入态联想建议，避免维护两套结构相同的展示模型。
 */
data class SearchGuessItem(
    /** 条目主标题，通常也是点击后用于发起搜索的关键词。 */
    val title: String,

    /** 条目补充信息，例如热度说明或分类提示。 */
    val meta: String
)

/**
 * 搜索结果条目模型
 *
 * 用于描述结果卡片渲染所需的核心信息，供结果列表按统一样式展示。
 */
data class SearchResultItem(
    /** 当前结果所属筛选分类。 */
    val filter: ResultFilter,

    /** 结果主标题。 */
    val title: String,

    /** 结果摘要或副标题信息。 */
    val subtitle: String,

    /** 结果的补充元信息，例如作者、时间或热度说明。 */
    val meta: String,

    /** 结果卡片顶部展示的标签文案。 */
    val badge: String,

    /** 标签背景色资源标识。 */
    val badgeColorRes: Int
)

/**
 * 搜索页面展示模式
 *
 * 用于区分默认态、联想建议态和结果态，界面层会根据该枚举切换对应内容区块。
 */
enum class SearchScreenMode {
    /** 默认态，展示历史记录、猜你想搜和语音入口。 */
    DEFAULT,

    /** 联想建议态，展示与输入关键词相关的推荐词。 */
    SUGGESTION,

    /** 搜索结果态，展示筛选栏和结果列表。 */
    RESULT
}

/**
 * 搜索结果筛选类型
 *
 * 定义结果页支持的分类维度，标签文案直接用于界面展示。
 */
enum class ResultFilter(val label: String) {
    /** 展示全部类型结果。 */
    ALL("综合"),

    /** 仅展示笔记类结果。 */
    NOTES("笔记"),

    /** 仅展示用户类结果。 */
    USERS("用户"),

    /** 仅展示话题类结果。 */
    TOPICS("话题"),

    /** 仅展示商品类结果。 */
    GOODS("商品")
}
