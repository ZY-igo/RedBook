/**
 * 文件说明：SearchViewModel.kt
 * 作用：负责搜索页面的状态组织、数据加载与用户操作响应。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhengyang.redbook.data.repository.SearchRepository
import com.zhengyang.redbook.utils.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 搜索页面状态管理类
 *
 * 负责串联搜索仓库与界面层，将搜索历史、猜你想搜、联想词和结果列表整理为统一的界面状态。
 * 该类只维护搜索场景需要的 UI 数据，不直接处理视图渲染，由 [SearchActivity] 负责消费状态并更新界面。
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    /** 搜索数据仓库，负责提供历史、联想和结果数据。 */
    private val searchRepository: SearchRepository
) : ViewModel() {

    /** 搜索页面内部可变状态流，仅允许当前 ViewModel 写入。 */
    private val _uiState = MutableStateFlow(SearchUiState())

    /** 对外暴露的只读状态流，供界面层订阅并渲染。 */
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        loadInitial()
    }

    /**
     * 加载搜索页默认态所需数据
     *
     * 用于进入页面或清空输入后恢复默认展示，包含历史记录、猜你想搜和默认筛选状态。
     *
     * @return 无返回值，执行后会异步更新 [uiState]。
     */
    fun loadInitial() {
        viewModelScope.launch {
            // 后端不可用时兜住异常，保留默认空状态，避免主线程崩溃。
            runCatching {
                _uiState.value = _uiState.value.copy(
                    historyItems = searchRepository.getHistory(),
                    guessItems = searchRepository.getGuessItems(),
                    suggestionItems = emptyList(),
                    resultItems = emptyList(),
                    currentQuery = "",
                    currentFilter = ResultFilter.ALL,
                    screenMode = SearchScreenMode.DEFAULT
                )
            }.onFailure { error ->
                AppLogger.e(
                    tag = "SearchViewModel",
                    message = "loadInitial failed",
                    throwable = error
                )
            }
        }
    }

    /**
     * 响应搜索框输入变化
     *
     * 当输入内容为空时恢复默认态；当输入存在有效内容时加载联想词并切换到建议态。
     *
     * @param query 搜索输入框当前文本，允许包含首尾空白字符。
     * @return 无返回值，执行后会异步更新 [uiState]。
     */
    fun onQueryChanged(query: String) {
        val trimmed = query.trim()
        viewModelScope.launch {
            // 联想词请求可能因断网/后端不可用失败，这里兜住异常并降级为空建议列表。
            runCatching {
                _uiState.update { state ->
                    if (trimmed.isEmpty()) {
                        state.copy(
                            currentQuery = "",
                            suggestionItems = emptyList(),
                            resultItems = emptyList(),
                            currentFilter = ResultFilter.ALL,
                            screenMode = SearchScreenMode.DEFAULT
                        )
                    } else {
                        state.copy(
                            currentQuery = trimmed,
                            suggestionItems = searchRepository.getSuggestions(trimmed),
                            screenMode = SearchScreenMode.SUGGESTION
                        )
                    }
                }
            }.onFailure { error ->
                AppLogger.e(
                    tag = "SearchViewModel",
                    message = "onQueryChanged failed, query=$trimmed",
                    throwable = error
                )
                if (trimmed.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            currentQuery = trimmed,
                            suggestionItems = emptyList(),
                            screenMode = SearchScreenMode.SUGGESTION
                        )
                    }
                }
            }
        }
    }

    /**
     * 提交搜索请求并切换到结果页
     *
     * 提交前会先裁剪首尾空白字符。空查询会直接恢复默认态，非空查询会写入历史并拉取结果列表。
     *
     * @param query 用户提交的原始搜索关键词。
     * @return 无返回值，执行后会异步更新 [uiState]。
     */
    fun submitSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            loadInitial()
            return
        }

        viewModelScope.launch {
            // 搜索结果请求可能因断网/后端不可用失败，这里兜住异常，
            // 失败时仍切换到结果页但展示空结果，避免主线程崩溃。
            runCatching {
                _uiState.value = _uiState.value.copy(
                    historyItems = searchRepository.saveHistory(trimmed),
                    resultItems = searchRepository.getResults(trimmed),
                    currentQuery = trimmed,
                    currentFilter = ResultFilter.ALL,
                    screenMode = SearchScreenMode.RESULT
                )
            }.onFailure { error ->
                AppLogger.e(
                    tag = "SearchViewModel",
                    message = "submitSearch failed, query=$trimmed",
                    throwable = error
                )
                _uiState.update {
                    it.copy(
                        resultItems = emptyList(),
                        currentQuery = trimmed,
                        currentFilter = ResultFilter.ALL,
                        screenMode = SearchScreenMode.RESULT
                    )
                }
            }
        }
    }

    /**
     * 清空搜索历史
     *
     * 该操作会同步清理仓库中的历史记录，并立即将界面中的历史列表置空。
     *
     * @return 无返回值，执行后会异步更新 [uiState]。
     */
    fun clearHistory() {
        viewModelScope.launch {
            runCatching { searchRepository.clearHistory() }
                .onFailure { error ->
                    AppLogger.e(
                        tag = "SearchViewModel",
                        message = "clearHistory failed",
                        throwable = error
                    )
                }
            _uiState.update { it.copy(historyItems = emptyList()) }
        }
    }

    /**
     * 切换结果筛选项
     *
     * 仅更新当前筛选状态，不会重新发起搜索请求，结果过滤由界面层基于现有数据执行。
     *
     * @param filter 用户当前选中的结果筛选类型。
     * @return 无返回值，执行后会同步更新 [uiState]。
     */
    fun changeFilter(filter: ResultFilter) {
        _uiState.update { it.copy(currentFilter = filter) }
    }
}
