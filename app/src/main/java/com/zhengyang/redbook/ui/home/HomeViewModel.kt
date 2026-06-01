package com.zhengyang.redbook.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhengyang.redbook.data.model.NoteItem
import com.zhengyang.redbook.data.repository.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val homeRepository: HomeRepository = EmptyHomeRepository
) : ViewModel() {
    fun refresh() {
        viewModelScope.launch {
            val listContent = homeRepository.getListContent()
            _uiState.value = HomeUiState(listContent)
        }
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState : StateFlow<HomeUiState> = _uiState.asStateFlow()

    private object EmptyHomeRepository : HomeRepository {
        override suspend fun getListContent(): List<NoteItem> = emptyList()
    }
}
