package com.zhengyang.redbook.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhengyang.redbook.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        loadInitial()
    }

    fun loadInitial() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                historyItems = searchRepository.getHistory(),
                guessItems = searchRepository.getGuessItems(),
                suggestionItems = emptyList(),
                resultItems = emptyList(),
                currentQuery = "",
                currentFilter = ResultFilter.ALL,
                screenMode = SearchScreenMode.DEFAULT
            )
        }
    }

    fun onQueryChanged(query: String) {
        val trimmed = query.trim()
        viewModelScope.launch {
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
        }
    }

    fun submitSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            loadInitial()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                historyItems = searchRepository.saveHistory(trimmed),
                resultItems = searchRepository.getResults(trimmed),
                currentQuery = trimmed,
                currentFilter = ResultFilter.ALL,
                screenMode = SearchScreenMode.RESULT
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            searchRepository.clearHistory()
            _uiState.update { it.copy(historyItems = emptyList()) }
        }
    }

    fun changeFilter(filter: ResultFilter) {
        _uiState.update { it.copy(currentFilter = filter) }
    }
}
