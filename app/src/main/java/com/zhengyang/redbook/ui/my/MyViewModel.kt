package com.zhengyang.redbook.ui.my

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MyViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    init {
        loadProfileStats()
        loadInterestPeople()
    }

    private fun loadProfileStats() {
        _uiState.update {
            it.copy(stats = MyMockData.profileStats())
        }
    }

    private fun loadInterestPeople() {
        _uiState.update {
            it.copy(
                interestPeople = MyMockData.interestPeople(),
                isInterestSectionVisible = true
            )
        }
    }

    fun dismissInterestSection() {
        _uiState.update { it.copy(isInterestSectionVisible = false) }
    }
}
