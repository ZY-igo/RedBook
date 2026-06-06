/**
 * 文件说明： SearchUiModels.kt
 * 作用： 承载搜索场景相关的界面状态与交互逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.search

data class SearchUiState(
    val historyItems: List<String> = emptyList(),
    val guessItems: List<SearchGuessItem> = emptyList(),
    val suggestionItems: List<SearchGuessItem> = emptyList(),
    val resultItems: List<SearchResultItem> = emptyList(),
    val currentQuery: String = "",
    val currentFilter: ResultFilter = ResultFilter.ALL,
    val screenMode: SearchScreenMode = SearchScreenMode.DEFAULT
)

data class SearchGuessItem(
    val title: String,
    val meta: String
)

data class SearchResultItem(
    val filter: ResultFilter,
    val title: String,
    val subtitle: String,
    val meta: String,
    val badge: String,
    val badgeColorRes: Int
)

enum class SearchScreenMode {
    DEFAULT,
    SUGGESTION,
    RESULT
}

enum class ResultFilter(val label: String) {
    ALL("综合"),
    NOTES("笔记"),
    USERS("用户"),
    TOPICS("话题"),
    GOODS("商品")
}
